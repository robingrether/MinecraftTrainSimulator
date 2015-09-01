package de.robingrether.mcts;

import java.io.Serializable;

import org.bukkit.Location;
import org.bukkit.Material;

public class Substation implements Serializable {
	
	private static final long serialVersionUID = 6001418474808541475L;
	
	private final Location[] blocks = new Location[9];
	private final String name;
	private final int voltage;
	private boolean created = false, redstone = false, state = false;
	
	public Substation(String name, int voltage) {
		if(voltage < 1) {
			throw new IllegalArgumentException("Voltage may not be 0 or less!");
		}
		this.name = name;
		this.voltage = voltage;
	}
	
	public void turnOn() {
		state = true;
	}
	
	public void turnOff() {
		state = false;
	}
	
	public boolean isTurnedOn() {
		return state;
	}
	
	public String getName() {
		return name;
	}
	
	public int getVoltage() {
		return voltage;
	}
	
	public boolean isRedstoneBlockPlaced() {
		return redstone;
	}
	
	public boolean placeRedstoneBlock(Location location) {
		if(created || redstone) {
			return false;
		}
		blocks[0] = location.clone();
		blocks[1] = location.clone().add(0, 1, 0);
		blocks[2] = location.clone().add(0, 2, 0);
		redstone = true;
		return true;
	}
	
	public boolean isCreated() {
		return created;
	}
	
	public boolean isAt(Location location) {
		for(Location block : blocks) {
			if(block.equals(location)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isLeverAt(Location location) {
		return blocks[2].equals(location);
	}
	
	public boolean placeFence(Location location) {
		if(created || !redstone) {
			return false;
		}
		blocks[3] = location.clone();
		blocks[4] = location.clone().add(0, 1, 0);
		blocks[5] = location.clone().add(0, 2, 0);
		blocks[6] = location.clone().add(0, 3, 0);
		blocks[7] = location.clone().add(blocks[3].getBlockX() - blocks[0].getBlockX(), 2, blocks[3].getBlockZ() - blocks[0].getBlockZ());
		blocks[8] = location.clone().add(blocks[3].getBlockX() - blocks[0].getBlockX(), 3, blocks[3].getBlockZ() - blocks[0].getBlockZ());
		blocks[1].getBlock().setType(Material.IRON_BLOCK);
		blocks[2].getBlock().setType(Material.LEVER);
		blocks[2].getBlock().setData((byte)5);
		Material fence = blocks[3].getBlock().getType();
		blocks[4].getBlock().setType(fence);
		blocks[5].getBlock().setType(fence);
		blocks[6].getBlock().setType(fence);
		blocks[7].getBlock().setType(Material.IRON_FENCE);
		blocks[8].getBlock().setType(fence);
		created = true;
		return true;
	}
	
	public boolean delete() {
		if(!created) {
			return false;
		}
		blocks[2].getBlock().setType(Material.AIR);
		blocks[1].getBlock().setType(Material.AIR);
		blocks[4].getBlock().setType(Material.AIR);
		blocks[5].getBlock().setType(Material.AIR);
		blocks[6].getBlock().setType(Material.AIR);
		blocks[7].getBlock().setType(Material.AIR);
		blocks[8].getBlock().setType(Material.AIR);
		return true;
	}
	
	public String getLocationString() {
		if(created) {
			return String.format("%s (%s, %s, %s)", blocks[0].getWorld().getName(), blocks[0].getBlockX(), blocks[0].getBlockY(), blocks[0].getBlockZ());
		} else {
			return "no position, still in creation";
		}
	}
	
}