package cz.iqlandia.iqplanetarium.starshipoverlay;

import cz.iqlandia.iqplanetarium.starshipoverlay.ui.UIController;
import io.javalin.Javalin;

import java.io.Closeable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;

public class StarshipOvelay implements Closeable {
	private final Javalin app;
	private final UIController uic;
	private final HashMap<String, CacheData> cache = HashMap.newHashMap(10);

	public StarshipOvelay() {
		app = Javalin.create();

		uic = new UIController();

		serveStatic("/time", "/time.html");
		serveStatic("/vcr.ttf", "/vcr.ttf");

		app.start(7777);
	}

	private void serveStatic(String path, String resPath) {
		app.get(path, ctx -> ctx.async(()-> {
			if(cache.containsKey(path)) {
				CacheData cacheData = cache.get(path);
				if(!cacheData.ttl_over()) {
					ctx.result(cacheData.data());
					return;
				} else {
					byte[] data = storeInCache(path,resPath);
					ctx.result(data);
				}
			} else {
				byte[] data = storeInCache(path,resPath);
				ctx.result(data);
			}
		}));
	}

	private byte[] storeInCache(String path, String resPath) throws IOException {
		cache.remove(path);
		byte[] data = StarshipOvelay.class.getResourceAsStream(resPath).readAllBytes();
		CacheData cd = new CacheData(data, ZonedDateTime.now().plusMinutes(1));
		cache.put(path,cd);
		return data;
	}

	@Override
	public void close() throws IOException {
		app.stop();
	}

	public static void main(String[] args) {
		StarshipOvelay so = new StarshipOvelay();

		Runtime.getRuntime().addShutdownHook(new Thread(()-> {
			try {
				so.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}));
	}
}