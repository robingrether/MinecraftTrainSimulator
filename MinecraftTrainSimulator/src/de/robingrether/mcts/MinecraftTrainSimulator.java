package de.robingrether.mcts;

import java.io.File;
import java.util.LinkedList;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

import de.robingrether.mcts.Metrics.Graph;
import de.robingrether.mcts.Metrics.Plotter;
import de.robingrether.mcts.render.Images;

public class MinecraftTrainSimulator extends JavaPlugin {
	
	private static MinecraftTrainSimulator instance;
	private LinkedList<Train> trains = new LinkedList<Train>();
	private Metrics metrics;
	
	public void onEnable() {
		if(TrainCarts.maxVelocity < 1.0) {
			TrainCarts.maxVelocity = 1.0;
		}
		getServer().getPluginManager().registerEvents(new MCTSListener(), this);
		if(!new File("plugins/MinecraftTrainSimulator").isDirectory()) {
			new File("plugins/MinecraftTrainSimulator").mkdirs();
		}
		Images.init();
		try {
			metrics = new Metrics(this);
			Graph graph = metrics.createGraph("Trains");
			graph.addPlotter(new Plotter("Trains") {
				public int getValue() {
					return trains.size();
				}
			});
			metrics.start();
		} catch(Exception e) {
		}
		instance = this;
		getLogger().log(Level.INFO, "MinecraftTrainSimulator v" + getDescription().getVersion() + " enabled!");
	}
	
	public void onDisable() {
		terminateTrains();
		instance = null;
		getLogger().log(Level.INFO, "MinecraftTrainSimulator v" + getDescription().getVersion() + " disabled!");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = null;
		if(sender instanceof Player) {
			player = (Player)sender;
		}
		if(cmd.getName().equalsIgnoreCase("mcts")) {
			if(player == null) {
				sender.sendMessage(ChatColor.RED + "This command can only be executed as player.");
			} else {
				if(args.length == 0) {
					sendHelp(player);
				} else if(args[0].equalsIgnoreCase("addfuel")) {
					if(getTrain(player) == null) {
						sender.sendMessage(ChatColor.RED + "You are not in a train.");
					} else {
						Train train = getTrain(player);
						if(Train.isFuel(player.getItemInHand().getType())) {
							int fuel = player.getItemInHand().getAmount() * 1200;
							train.addFuel(fuel);
							player.setItemInHand(null);
							sender.sendMessage(ChatColor.GOLD + "Added fuel to the train.");
						} else {
							sender.sendMessage(ChatColor.RED + "You have to hold coal in your hand.");
						}
					}
				} else if(args[0].equalsIgnoreCase("control")) {
					if(getTrain(player) == null) {
						sender.sendMessage(ChatColor.RED + "You are not in a train.");
					} else {
						Train train = getTrain(player);
						if(train.canLead(player)) {
							train.setLeader(player);
							sender.sendMessage(ChatColor.GOLD + "You can now control the train.");
						} else {
							sender.sendMessage(ChatColor.RED + "You have to sit in the head or in the tail of the train.");
						}
					}
				} else if(args[0].equalsIgnoreCase("create")) {
					if(player.getVehicle() == null) {
						sender.sendMessage(ChatColor.RED + "You have to be in a cart to create a train.");
					} else {
						MinecartGroup minecarts = MinecartGroup.get(player.getVehicle());
						if(getTrain(minecarts) == null) {
							Train train = new Train(minecarts, createNewMap(player.getWorld()));
							trains.add(train);
							sender.sendMessage(ChatColor.GOLD + "Created train.");
						}
					}
				} else if(args[0].equalsIgnoreCase("fuel")) {
					if(getTrain(player) == null) {
						sender.sendMessage(ChatColor.RED + "You are not in a train.");
					} else {
						Train train = getTrain(player);
						sender.sendMessage(ChatColor.GOLD + "Fuel level: " + train.getFuel());
					}
				} else {
					sendHelp(player);
				}
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("df")) {
			executeDirectionChange(sender, player, 1);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("dn")) {
			executeDirectionChange(sender, player, 0);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("db")) {
			executeDirectionChange(sender, player, -1);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("p4")) {
			executeLeverChange(sender, player, 4);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("p3")) {
			executeLeverChange(sender, player, 3);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("p2")) {
			executeLeverChange(sender, player, 2);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("p1")) {
			executeLeverChange(sender, player, 1);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("neutral")) {
			executeLeverChange(sender, player, 0);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("b1")) {
			executeLeverChange(sender, player, -1);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("b2")) {
			executeLeverChange(sender, player, -2);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("b3")) {
			executeLeverChange(sender, player, -3);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("b4")) {
			executeLeverChange(sender, player, -4);
			return true;
		} else {
			return false;
		}
	}
	
	private void executeDirectionChange(CommandSender sender, Player player, int direction) {
		if(player == null) {
			sender.sendMessage(ChatColor.RED + "This command can only be executed as player.");
		} else {
			if(getTrain(player) == null) {
				sender.sendMessage(ChatColor.RED + "You are not in a train.");
			} else {
				Train train = getTrain(player);
				if(train.isLeader(player)) {
					train.setDirection(direction, true);
				} else {
					sender.sendMessage(ChatColor.RED + "You don't control that train.");
				}
			}
		}
	}
	
	private void executeLeverChange(CommandSender sender, Player player, int status) {
		if(player == null) {
			sender.sendMessage(ChatColor.RED + "This command can only be executed as player.");
		} else {
			if(getTrain(player) == null) {
				sender.sendMessage(ChatColor.RED + "You are not in a train.");
			} else {
				Train train = getTrain(player);
				if(train.isLeader(player)) {
					train.setCombinedLever(status, true);
				} else {
					sender.sendMessage(ChatColor.RED + "You don't control that train.");
				}
			}
		}
	}
	
	public Train getTrain(Player player) {
		return getTrain(player, false);
	}
	
	public Train getTrain(Player player, boolean leader) {
		if(leader) {
			for(Train train : trains) {
				if(train != null && train.getLeader() != null && train.getLeader().equals(player)) {
					return train;
				}
			}
		} else {
			if(player.getVehicle() != null) {
				return getTrain(MinecartGroup.get(player.getVehicle()));
			}
		}
		return null;
	}
	
	public Train getTrain(MinecartGroup minecarts) {
		for(Train train : trains) {
			if(train.getMinecarts().equals(minecarts)) {
				return train;
			}
		}
		return null;
	}
	
	private MapView createNewMap(World world) {
		MapView map = getServer().createMap(world);
		return map;
	}
	
	private void terminateTrains() {
		for(Train train : trains) {
			train.terminate();
		}
	}
	
	private void sendHelp(Player player) {
		player.sendMessage(ChatColor.AQUA + "MinecraftTrainSimulator - Help");
		player.sendMessage(ChatColor.GOLD + " /mcts addfuel - Add fuel to a train");
		player.sendMessage(ChatColor.GOLD + " /mcts control - Control a train");
		player.sendMessage(ChatColor.GOLD + " /mcts create  - Create a train");
		player.sendMessage(ChatColor.GOLD + " /mcts fuel    - See a train's fuel level");
		player.sendMessage(ChatColor.GOLD + " /df  /dn  /db - Change the direction");
		player.sendMessage(ChatColor.GOLD + " /p4  /p3  /p2  /p1  /neutral  /b1  /b2  /b3  /b4");
		player.sendMessage(ChatColor.GOLD + " - Control the accelerator and brake");
	}
	
	public static MinecraftTrainSimulator getInstance() {
		return instance;
	}
	
}