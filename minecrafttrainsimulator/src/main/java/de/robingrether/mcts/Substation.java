package de.robingrether.mcts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Substation {
	
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
		MinecraftTrainSimulator.getInstance().updateCatenary();
	}
	
	public void turnOff() {
		state = false;
		MinecraftTrainSimulator.getInstance().updateCatenary();
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
	
	public Location getRedstoneBlockLocation() {
		return blocks[0].clone();
	}
	
	public Location getIronFenceLocation() {
		return blocks[7].clone();
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
	
	public String toString() {
		return String.format("%s %s (%s) - %s (%s, %s, %s), (%s, %s, %s)", name, voltage, isTurnedOn() ? "on" : "off", blocks[0].getWorld().getName(), blocks[0].getBlockX(), blocks[0].getBlockY(), blocks[0].getBlockZ(), blocks[3].getBlockX(), blocks[3].getBlockY(), blocks[3].getBlockZ());
	}
	
	private static Pattern pattern = Pattern.compile("^(.+) ([0-9]+) \\((off|on)\\) - (.+) \\((-?[0-9]+), (-?[0-9]+), (-?[0-9]+)\\), \\((-?[0-9]+), (-?[0-9]+), (-?[0-9]+)\\)");
	
	public static Substation fromString(String source) {
		Matcher matcher = pattern.matcher(source);
		if(matcher.matches()) {
			Substation substation = new Substation(matcher.group(1), Integer.parseInt(matcher.group(2)));
			World world = Bukkit.getWorld(matcher.group(4));
			if(world == null) {
				return null;
			} else {
				substation.placeRedstoneBlock(new Location(world, Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(6)), Integer.parseInt(matcher.group(7))));
				substation.placeFence(new Location(world, Integer.parseInt(matcher.group(8)), Integer.parseInt(matcher.group(9)), Integer.parseInt(matcher.group(10))));
				switch(matcher.group(3)) {
					case "on":
						substation.turnOn();
						substation.blocks[2].getBlock().setData((byte)13);
						break;
					case "off":
						substation.turnOff();
						break;
				}
				return substation;
			}
		} else {
			return null;
		}
	}
	
}