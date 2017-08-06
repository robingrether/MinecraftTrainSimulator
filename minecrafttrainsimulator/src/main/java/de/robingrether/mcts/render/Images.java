package de.robingrether.mcts.render;

import java.awt.Image;
import java.io.InputStream;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;

public class Images {
	
	private static Image[] images = new Image[5];
	private static final String[] files = {"display.png", "fire_off.png", "fire_on.png", "power_off.png", "power_on.png"};
	
	public static void init() {
		try {
			for(int i = 0; i < files.length; i++) {
				images[i] = ImageIO.read(getInputStream(files[i]));
			}
		} catch(Exception e) {
			Bukkit.getPluginManager().getPlugin("MinecraftTrainSimulator").getLogger().log(Level.SEVERE, "Cannot load image files.", e);
		}
	}
	
	public static Image getImage(int i) {
		return images[i];
	}
	
	private static InputStream getInputStream(String name) {
		return Images.class.getResourceAsStream(name);
	}
	
	public static void main(String[] args) {
		init();
	}
	
}