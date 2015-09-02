package de.robingrether.mcts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

import de.robingrether.mcts.Metrics.Graph;
import de.robingrether.mcts.Metrics.Plotter;
import de.robingrether.mcts.render.Images;
import de.robingrether.util.StringUtil;

public class MinecraftTrainSimulator extends JavaPlugin {
	
	public static final File directory = new File("plugins/MinecraftTrainSimulator");
	private static MinecraftTrainSimulator instance;
	
	private Set<Train> trains = new HashSet<Train>();
	Map<String, Substation> substations = new ConcurrentHashMap<String, Substation>();
	Set<Location> catenary;
	
	private MCTSListener listener;
	private Metrics metrics;
	
	public void onEnable() {
		instance = this;
		if(TrainCarts.maxVelocity < 1.0) {
			TrainCarts.maxVelocity = 1.0;
		}
		listener = new MCTSListener(this);
		getServer().getPluginManager().registerEvents(listener, this);
		checkDirectory();
		loadData();
		Images.init();
		try {
			metrics = new Metrics(this);
			Graph graph1 = metrics.createGraph("Trains");
			graph1.addPlotter(new Plotter("steam trains") {
				public int getValue() {
					int count = 0;
					for(Train train : trains) {
						if(train instanceof SteamTrain) {
							count++;
						}
					}
					return count;
				}
			});
			graph1.addPlotter(new Plotter("electric trains") {
				public int getValue() {
					int count = 0;
					for(Train train : trains) {
						if(train instanceof ElectricTrain) {
							count++;
						}
					}
					return count;
				}
			});
			Graph graph2 = metrics.createGraph("Substations");
			graph2.addPlotter(new Plotter("substations") {
				public int getValue() {
					return substations.size();
				}
			});
			metrics.start();
		} catch(Exception e) {
		}
		updateCatenary();
		getLogger().log(Level.INFO, "MinecraftTrainSimulator v" + getDescription().getVersion() + " enabled!");
	}
	
	public void onDisable() {
		terminateTrains();
		saveData();
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
						if(train instanceof SteamTrain) {
							if(SteamTrain.isFuel(player.getItemInHand().getType())) {
								int fuel = player.getItemInHand().getAmount() * 1200;
								train.addFuel(fuel);
								player.setItemInHand(null);
								sender.sendMessage(ChatColor.GOLD + "Added fuel to the train.");
							} else {
								sender.sendMessage(ChatColor.RED + "You have to hold coal in your hand.");
							}
						} else {
							sender.sendMessage(ChatColor.RED + "Electric trains don't need fuel.");
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
					if(!(player.getVehicle() instanceof Minecart)) {
						sender.sendMessage(ChatColor.RED + "You have to be in a cart to create a train.");
					} else {
						if(args.length < 2) {
							sender.sendMessage(ChatColor.RED + "Wrong usage: /mcts create <coal/electric>");
						} else if(StringUtil.equalsIgnoreCase(args[1], "coal", "steam")) {
							MinecartGroup minecarts = MinecartGroup.get(player.getVehicle());
							if(getTrain(minecarts) == null) {
								Train train = new SteamTrain(minecarts, createNewMap(player.getWorld()));
								trains.add(train);
								sender.sendMessage(ChatColor.GOLD + "Created steam train.");
							} else {
								sender.sendMessage(ChatColor.RED + "This already is a train.");
							}
						} else if(args[1].equalsIgnoreCase("electric")) {
							MinecartGroup minecarts = MinecartGroup.get(player.getVehicle());
							if(getTrain(minecarts) == null) {
								Train train = new ElectricTrain(minecarts, createNewMap(player.getWorld()));
								trains.add(train);
								sender.sendMessage(ChatColor.GOLD + "Created electric train.");
							} else {
								sender.sendMessage(ChatColor.RED + "This already is a train.");
							}
						} else {
							sender.sendMessage(ChatColor.RED + "Wrong usage: /mcts create <coal/electric>");
						}
					}
				} else if(args[0].equalsIgnoreCase("fuel")) {
					if(getTrain(player) == null) {
						sender.sendMessage(ChatColor.RED + "You are not in a train.");
					} else {
						Train train = getTrain(player);
						if(train instanceof SteamTrain) {
							sender.sendMessage(ChatColor.GOLD + "Fuel level: " + train.getFuel());
						} else {
							sender.sendMessage(ChatColor.GOLD + "Has energy: " + (train.hasFuel() ? "yes" : "no"));
						}
					}
				} else {
					sendHelp(player);
				}
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("substation")) {
			if(player == null) {
				sender.sendMessage(ChatColor.RED + "This command can only be executed as player.");
			} else {
				if(args.length == 0) {
					sendHelp(player);
				} else if(args[0].equalsIgnoreCase("create")) {
					if(args.length < 3) {
						sender.sendMessage(ChatColor.RED + "Wrong usage: /substation create <name> <voltage>");
					} else {
						if(substations.containsKey(args[1].toLowerCase(Locale.ENGLISH))) {
							sender.sendMessage(ChatColor.RED + "That name is already used.");
						} else {
							try {
								int voltage = Integer.parseInt(args[2]);
								if(voltage > 0) {
									Substation substation = new Substation(args[1].toLowerCase(Locale.ENGLISH), voltage);
									listener.substations.put(player.getName().toLowerCase(Locale.ENGLISH), substation);
									sender.sendMessage(ChatColor.GOLD + "Place a redstone block with a distance of one block next to your rails.");
								} else {
									sender.sendMessage(ChatColor.RED + "The voltage may not be 0 or less.");
								}
							} catch(NumberFormatException e) {
								sender.sendMessage(ChatColor.RED + "The voltage must be a valid integer.");
							}
						}
					}
				} else if(args[0].equalsIgnoreCase("list")) {
					sender.sendMessage(ChatColor.AQUA + "Substations");
					for(Substation substation : substations.values()) {
						sender.sendMessage(ChatColor.GOLD + " '" + substation.getName() + "' " + substation.getVoltage() + "V (" + (substation.isTurnedOn() ? ChatColor.GREEN + "on" : ChatColor.RED + "off") + ChatColor.GOLD + ") at " + substation.getLocationString());
					}
				} else if(args[0].equalsIgnoreCase("remove")) {
					if(args.length < 2) {
						sender.sendMessage(ChatColor.RED + "Wrong usage: /substation remove <name>");
					} else {
						Substation substation = substations.remove(args[1].toLowerCase(Locale.ENGLISH));
						if(substation == null) {
							sender.sendMessage(ChatColor.RED + "There is no substation called '" + args[1] + "'");
						} else {
							substation.delete();
							sender.sendMessage(ChatColor.GOLD + "Removed substation '" + args[1] + "'");
						}
					}
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
	
	private void checkDirectory() {
		if(!directory.isDirectory()) {
			directory.mkdir();
		}
	}
	
	private void loadData() {
		File dataFile = new File(directory, "substations.dat");
		if(dataFile.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)));
				String line;
				while((line = reader.readLine()) != null) {
					Substation substation = Substation.fromString(line);
					if(substation != null) {
						substations.put(substation.getName(), substation);
					}
				}
				reader.close();
			} catch(Exception e) {
				getLogger().log(Level.SEVERE, "Cannot load substations.", e);
			}
		}
	}
	
	private void saveData() {
		File dataFile = new File(directory, "substations.dat");
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile)));
			for(Substation substation : substations.values()) {
				writer.write(substation.toString() + "\n");
			}
			writer.flush();
			writer.close();
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Cannot save substations.", e);
		}
	}
	
	public void updateCatenary() {
		catenary = new HashSet<Location>();
		for(Substation substation : substations.values()) {
			if(!substation.isTurnedOn()) {
				continue;
			}
			Set<Block> tested = new HashSet<Block>(), to_test;
			int distance = 1;
			catenary.add(substation.getIronFenceLocation());
			tested.add(substation.getIronFenceLocation().getBlock());
			while(distance < substation.getVoltage() / 10) {
				to_test = new HashSet<Block>();
				for(Block block : tested) {
					to_test.addAll(Arrays.asList(getNeighbors(block)));
				}
				tested = new HashSet<Block>();
				for(Block block : to_test) {
					if(block.getType().equals(Material.IRON_FENCE)) {
						catenary.add(block.getLocation());
						tested.add(block);
					}
				}
				distance++;
			}
		}
	}
	
	private Block[] getNeighbors(Block block) {
		return new Block[]{block.getRelative(BlockFace.UP), block.getRelative(BlockFace.DOWN), block.getRelative(BlockFace.EAST), block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH)};
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
		player.sendMessage(ChatColor.GOLD + " /mcts addfuel                       - Add fuel to a train");
		player.sendMessage(ChatColor.GOLD + " /mcts control                       - Control a train");
		player.sendMessage(ChatColor.GOLD + " /mcts create <coal/electric>        - Create a train");
		player.sendMessage(ChatColor.GOLD + " /mcts fuel                          - See a train's fuel level");
		player.sendMessage(ChatColor.GOLD + " /substation create <name> <voltage> - Create a substation");
		player.sendMessage(ChatColor.GOLD + " /substation list                    - List all substations");
		player.sendMessage(ChatColor.GOLD + " /substation remove <name>           - Remove a substation");
		player.sendMessage(ChatColor.GOLD + " /df  /dn  /db - Change the direction");
		player.sendMessage(ChatColor.GOLD + " /p4  /p3  /p2  /p1  /neutral  /b1  /b2  /b3  /b4");
		player.sendMessage(ChatColor.GOLD + " - Control the accelerator and brake");
	}
	
	public static MinecraftTrainSimulator getInstance() {
		return instance;
	}
	
}