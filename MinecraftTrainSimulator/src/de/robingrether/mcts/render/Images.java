package de.robingrether.mcts.render;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class Images {
	
	private static Image[] images = new Image[4];
	private static final String[] files = {"banner.png", "display.png", "fire_off.png", "fire_on.png"};
	
	public static void init() {
		try {
			if(getDirFile(files[0]).exists()) {
				images[0] = ImageIO.read(getDirFile(files[0]));
			} else {
				try {
					saveImage(getInputStream(files[0]), getDirFile(files[0]));
					images[0] = ImageIO.read(getDirFile(files[0]));
				} catch(Exception e) {
					images[0] = ImageIO.read(getInputStream(files[0]));
				}
			}
			for(int i = 1; i < files.length; i++) {
				images[i] = ImageIO.read(getInputStream(files[i]));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Image getImage(int i) {
		return images[i];
	}
	
	private static File getDirFile(String name) {
		return new File("plugins/MinecraftTrainSimulator/" + name);
	}
	
	private static InputStream getInputStream(String name) {
		return Images.class.getResourceAsStream(name);
	}
	
	private static void saveImage(InputStream from, File to) throws IOException {
		BufferedImage image = ImageIO.read(from);
		to.createNewFile();
		ImageIO.write(image, "png", to);
	}
	
	public static void main(String[] args) {
		init();
	}
	
}