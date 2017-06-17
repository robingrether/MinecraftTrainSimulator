package de.robingrether.mcts;

import org.bukkit.map.MapView;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;

public class ElectricTrain extends Train {
	
	public ElectricTrain(MinecartGroup minecarts, MapView controlPanel) {
		super(minecarts, controlPanel);
	}
	
	public double getSpeedLimit() {
		return minecarts.getProperties().getSpeedLimit();
	}
	
	public boolean addFuel(int fuel) {
		return false;
	}
	
	public boolean consumeFuel() {
		for(MinecartMember<?> minecart : minecarts) {
			if(MinecraftTrainSimulator.getInstance().catenary.contains(minecart.getBlock(0, 2, 0).getLocation())) {
				return true;
			}
		}
		return false;
	}
	
	public int getFuel() {
		return 0;
	}
	
	public boolean hasFuel() {
		for(MinecartMember<?> minecart : minecarts) {
			if(MinecraftTrainSimulator.getInstance().catenary.contains(minecart.getBlock(0, 2, 0).getLocation())) {
				return true;
			}
		}
		return false;
	}
	
}