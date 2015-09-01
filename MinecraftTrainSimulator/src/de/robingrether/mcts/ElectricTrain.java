package de.robingrether.mcts;

import org.bukkit.map.MapView;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

public class ElectricTrain extends Train {
	
	public ElectricTrain(MinecartGroup minecarts, MapView controlPanel) {
		super(minecarts, controlPanel);
	}
	
	public boolean addFuel(int fuel) {
		return false;
	}
	
	public boolean consumeFuel() {
		return false; // TODO: check for powered
	}
	
	public int getFuel() {
		return 0; // TODO: check for powered
	}
	
	public boolean hasFuel() {
		return false; // TODO: check for powered
	}
	
}