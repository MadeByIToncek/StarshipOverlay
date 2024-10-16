package cz.iqlandia.iqplanetarium.starshipoverlay;

import cz.iqlandia.iqplanetarium.starshipoverlay.twinkly.Twinkly;
import cz.iqlandia.iqplanetarium.starshipoverlay.ui.UIController;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class StarshipOvelay implements Closeable {
	private final Javalin app;
	private final UIController uic;
	private final Twinkly twinkly;
	public final Config cfg;
	private final HashMap<String, CacheData> cache = HashMap.newHashMap(10);
	private final Logger log = LoggerFactory.getLogger(StarshipOvelay.class);
	private final ArrayList<WsContext> wsContexts = new ArrayList<>();

	public StarshipOvelay() throws IOException, ClassNotFoundException, InterruptedException {
		cfg = createOrLoadConfig();
		app = Javalin.create(cfg-> {
			cfg.useVirtualThreads = true;
			cfg.startupWatcherEnabled = false;

		});

		twinkly = new Twinkly("http://192.168.99.174");
		try {
			twinkly.login();
		} catch (Exception ignored) {}

		uic = new UIController(cfg.t0, cfg, twinkly, (color)-> {
			ArrayList<WsContext> for_removal = new ArrayList<>();
			for (WsContext wsContext : wsContexts) {
				try {
					wsContext.send("#" + Integer.toHexString(color.getRGB()).substring(2));
				} catch (Exception e) {
					for_removal.add(wsContext);
				}
			}
			for (WsContext wsContext : for_removal) {
				wsContexts.remove(wsContext);
			}
		});

		serveStatic("/time", "/time.html", ContentType.TEXT_HTML);
		serveStatic("/vcr.ttf", "/vcr.ttf", ContentType.FONT_TTF);
		serveStatic("/colorfade.js", "/colorfade.js", ContentType.TEXT_JS);

		app.get("/api/t0", ctx -> {
			ctx.status(200).contentType(ContentType.TEXT_PLAIN).result(uic.getLaunchT0().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxxx")));
		});
		app.ws("/ws/color", cf -> {
			cf.onConnect(ctx -> {
				ctx.enableAutomaticPings();
				ctx.session.setIdleTimeout(Duration.of(7, ChronoUnit.DAYS));
				wsContexts.add(ctx);
				log.info("WS Connected!");
			});
		});

		app.start(7777);
	}

	private Config createOrLoadConfig() throws IOException, ClassNotFoundException {
		if (!new File("./config.so").exists()) {
			log.warn("Configuration missing, recreating!");
			return new Config();
		} else {
			try (FileInputStream fis = new FileInputStream("./config.so");
				 ObjectInputStream ois = new ObjectInputStream(fis)) {

				Object o = ois.readObject();
				if (o instanceof Config) {
					log.info("Configuration was loaded successfully!");
					return (Config) o;
				} else {
					log.error("Configuration was not loaded successfully, recreating!");
					ois.close();
					fis.close();
					new File("./config.so").delete();
					return new Config();
				}
			} catch (Exception e) {
				log.error("Configuration was old, recreating!");
				new File("./config.so").delete();
				return new Config();
			}
		}
	}

	private void saveConfig() throws IOException {
		log.info("Saving!");
		FileOutputStream fos = new FileOutputStream("./config.so");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(cfg);
		fos.close();
		oos.close();
		log.info("Saved!");
	}

	private void serveStatic(String path, String resPath, ContentType ctype) {
		app.get(path, ctx -> ctx.async(() -> {
			if (cache.containsKey(path)) {
				CacheData cacheData = cache.get(path);
				if (!cacheData.ttl_over()) {
					ctx.status(200).contentType(ctype).result(cacheData.data());
				} else {
					byte[] data = storeInCache(path, resPath);
					ctx.status(200).contentType(ctype).result(data);
				}
			} else {
				byte[] data = storeInCache(path, resPath);
				ctx.status(200).contentType(ctype).result(data);
			}
		}));
	}

	private byte[] storeInCache(String path, String resPath) throws IOException {
		cache.remove(path);
		byte[] data = StarshipOvelay.class.getResourceAsStream(resPath).readAllBytes();
		CacheData cd = new CacheData(data, ZonedDateTime.now().plusMinutes(1));
		cache.put(path, cd);
		return data;
	}

	@Override
	public void close() throws IOException {
		twinkly.close();
		uic.close();
		app.stop();
		saveConfig();
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		StarshipOvelay so = new StarshipOvelay();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				so.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}));
	}
}