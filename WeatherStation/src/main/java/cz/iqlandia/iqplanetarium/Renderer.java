package cz.iqlandia.iqplanetarium;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Renderer extends JComponent {
	private static final Logger log = LoggerFactory.getLogger(Renderer.class);

	private BufferedImage goes;
	private BufferedImage goes_overlay;
	private BufferedImage eumetsat;
	private BufferedImage eumetsat_overlay;

	public Renderer() throws InterruptedException {
		ExecutorService es = Executors.newFixedThreadPool(4);

		CountDownLatch cdl = new CountDownLatch(4);

		es.submit(()-> {
			//goes = getGOESClouds();
			cdl.countDown();
		});
		es.submit(()-> {
			goes_overlay = getGOESOverlay();
			cdl.countDown();
		});

		es.submit(()-> {
			eumetsat = getEumetsatClouds();
			cdl.countDown();
		});
		es.submit(()-> {
			eumetsat_overlay = getEumetsatOverlay();
			cdl.countDown();
		});

		es.shutdown();
		cdl.await();
	}

	@Override
	public void paint(Graphics g) {
		g.drawImage(eumetsat,0,0,1920,1080,null);
		super.paint(g);
	}

	public @Nullable BufferedImage getEumetsatClouds() {
		try {
			log.info("Loading EumetsatClouds west");
			BufferedImage west = ImageIO.read(new URL("https://eumetview.eumetsat.int/static-images/latestImages/EUMETSAT_MSGIODC_IR108Color_WestIndianOcean.jpg"));
			log.info("Loading EumetsatClouds east");
			BufferedImage east = ImageIO.read(new URL("https://eumetview.eumetsat.int/static-images/latestImages/EUMETSAT_MSGIODC_IR108Color_EastIndianOcean.jpg"));
			log.info("Stitching EumetsatClouds");
			BufferedImage img = new BufferedImage(2155,1304,BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g2 = img.createGraphics();

			g2.drawImage(west,0,0,null);
			g2.drawImage(east,671,0,null);

			g2.dispose();
			log.info("Cropping EumetsatClouds");
			return img.getSubimage(0,0, 2155, 1212);
		} catch (Exception e) {
			log.error("getEumetsatClouds()",e);
			return null;
		}
	}

	public @Nullable BufferedImage getGOESClouds() {
		try {
			log.info("Loading GOESClouds");
			BufferedImage img = ImageIO.read(new URL("https://cdn.star.nesdis.noaa.gov/GOES16/GLM/SECTOR/gm/EXTENT3/4000x4000.jpg"));
			log.info("Cropping GOESClouds");
			return img.getSubimage(86,931, 3840, 2160);
		} catch (Exception e) {
			log.error("getGOESClouds()",e);
			return null;
		}
	}

	private @Nullable BufferedImage readInternalImage(String resname) {

		try {
			log.info("Loading image /{}",resname);
			InputStream is = this.getClass().getResourceAsStream("/" + resname);
			assert is != null;
			return ImageIO.read(is);
		} catch (Exception e) {
			log.error("readInternalImage()",e);
			return null;
		}
	}

	private @Nullable BufferedImage getEumetsatOverlay() {
		return readInternalImage("goes_trajectory_overlay.png");
	}

	private @Nullable BufferedImage getGOESOverlay() {
		return readInternalImage("goes_trajectory_overlay.png");
	}
}
