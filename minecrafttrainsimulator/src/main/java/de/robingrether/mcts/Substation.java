package de.robingrether.mcts;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Powerable;

public class Substation {
	
	public static int CATENARY_HEIGHT = 2;
	public static Material CATENARY_MATERIAL = Material.IRON_BARS;
	public static Material SUBSTATION_BOTTOM = Material.REDSTONE_BLOCK;
	public static Material SUBSTATION_TOP = Material.IRON_BLOCK;
	public static List<Material> SUBSTATION_SUPPORT = Arrays.asList(Material.OAK_FENCE, Material.BIRCH_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE, Material.JUNGLE_FENCE, Material.SPRUCE_FENCE, Material.NETHER_BRICK_FENCE);
	
	/*
	 * [0] -> redstoneBlock
	 * [1] -> fenceBase
	 * [2] -> ironBlock
	 * [3] -> lever
	 * [4] -> ironFence
	 * [5...] -> catenarySupport
	 */
	private final Location[] blocks = new Location[7 + CATENARY_HEIGHT];
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
		return blocks[3].equals(location);
	}
	
	public Location getRedstoneBlockLocation() {
		return blocks[0].clone();
	}
	
	public Location getIronFenceLocation() {
		return blocks[4].clone();
	}
	
	public boolean placeFence(Location location) {
		if(created || !redstone) {
			return false;
		}
		blocks[1] = location.clone();
		blocks[2] = blocks[0].clone().add(0, 1, 0);
		blocks[3] = blocks[0].clone().add(0, 2, 0);
		blocks[4] = blocks[1].clone().add(blocks[1].getBlockX() - blocks[0].getBlockX(), CATENARY_HEIGHT, blocks[1].getBlockZ() - blocks[0].getBlockZ());
		for(int i = 5; i < blocks.length - 1; i++) {
			blocks[i] = blocks[1].clone().add(0, i - 4, 0);
		}
		blocks[blocks.length - 1] = blocks[4].clone().add(0, 1, 0);
		
		blocks[0].getBlock().setType(SUBSTATION_BOTTOM);
		blocks[2].getBlock().setType(SUBSTATION_TOP);
		blocks[3].getBlock().setBlockData(Bukkit.createBlockData("minecraft:lever[face=floor,powered=false]"));
		blocks[4].getBlock().setType(CATENARY_MATERIAL);
		
		Material fence = blocks[1].getBlock().getType();
		for(int i = 5; i < blocks.length; i++) {
			blocks[i].getBlock().setType(fence);
		}
		
		created = true;
		return true;
	}
	
	public boolean delete() {
		if(!created) {
			return false;
		}
		for(int i = 2; i < blocks.length; i++) {
			blocks[i].getBlock().setType(Material.AIR);
		}
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
		return String.format("%s %s (%s) - %s (%s, %s, %s), (%s, %s, %s)", name, voltage, isTurnedOn() ? "on" : "off", blocks[0].getWorld().getName(), blocks[0].getBlockX(), blocks[0].getBlockY(), blocks[0].getBlockZ(), blocks[1].getBlockX(), blocks[1].getBlockY(), blocks[1].getBlockZ());
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
						Powerable lever = (Powerable)substation.blocks[3].getBlock().getBlockData();
						lever.setPowered(true);
						substation.blocks[3].getBlock().setBlockData(lever);
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