package de.robingrether.mcts;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.map.MapView;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

public class SteamTrain extends Train {
	
	private int fuel;
	
	public SteamTrain(MinecartGroup minecarts, MapView controlPanel) {
		super(minecarts, controlPanel);
		this.fuel = 0;
	}
	
	public boolean addFuel(int fuel) {
		if(fuel > 0) {
			this.fuel += fuel;
			return true;
		}
		return false;
	}
	
	public int getFuel() {
		return fuel;
	}
	
	public boolean consumeFuel() {
		fuel -= status;
		if(fuel < 0) {
			fuel = 0;
			return false;
		} else {
			return true;
		}
	}
	
	public boolean hasFuel() {
		return fuel - status >= 0;
	}
	
	public static boolean isFuel(Material material) {
		return fuels.contains(material);
	}
	
	public static boolean newFuel(Material material) {
		return fuels.add(material);
	}
	
	private static final HashSet<Material> fuels = new HashSet<Material>();
	
	static {
		newFuel(Material.COAL);
	}
	
}