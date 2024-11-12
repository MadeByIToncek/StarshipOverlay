package cz.iqlandia.iqplanetarium;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class WeatherStation {
	public static void main(String[] args) throws InterruptedException {
		Renderer r = new Renderer();

		JFrame frame = new JFrame("WeatherStation display");

		frame.setResizable(false);
		frame.setSize(1920,1080);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.add(r);


		frame.setVisible(true);
		frame.repaint();
	}
}