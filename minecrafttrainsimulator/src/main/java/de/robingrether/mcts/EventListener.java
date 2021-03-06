package de.robingrether.mcts;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

import de.robingrether.mcts.io.UpdateCheck;
import de.robingrether.util.ObjectUtil;

public class EventListener implements Listener {
	
	private MinecraftTrainSimulator plugin;
	Map<String, Substation> substations = new ConcurrentHashMap<String, Substation>();
	
	public EventListener(MinecraftTrainSimulator plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if(player.hasPermission("MCTS.update") && plugin.configuration.UPDATE_CHECK) {
			plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new UpdateCheck(plugin, player, plugin.configuration.UPDATE_DOWNLOAD), 20L);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		if(!event.isCancelled()) {
			Block placed = event.getBlockPlaced();
			Player player = event.getPlayer();
			if(placed.getType().equals(Substation.SUBSTATION_BOTTOM)) {
				if(substations.containsKey(player.getName().toLowerCase(Locale.ENGLISH))) {
					Substation substation = substations.get(player.getName().toLowerCase(Locale.ENGLISH));
					if(!substation.isRedstoneBlockPlaced()) {
						substation.placeRedstoneBlock(placed.getLocation());
						player.sendMessage(ChatColor.GOLD + "Now place a fence (wooden or netherbrick) next to the rail.");
					}
				}
			} else if(ObjectUtil.equals(placed.getType(), Substation.SUBSTATION_SUPPORT.toArray())) {
				if(substations.containsKey(player.getName().toLowerCase(Locale.ENGLISH))) {
					Substation substation = substations.get(player.getName().toLowerCase(Locale.ENGLISH));
					if(substation.isRedstoneBlockPlaced()) {
						if(ObjectUtil.equals(substation.getRedstoneBlockLocation().subtract(placed.getLocation()).toVector(), new Vector(1, 0, 0), new Vector(-1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1))) {
							substation.placeFence(placed.getLocation());
							plugin.substations.put(substation.getName(), substation);
							substations.remove(player.getName().toLowerCase(Locale.ENGLISH));
							player.sendMessage(ChatColor.GOLD + "Created substation. Move the lever to turn it on.");
						} else {
							event.setCancelled(true);
							player.sendMessage(ChatColor.RED + "The fence must be next to the redstone block.");
						}
					}
				}
			} else if(placed.getType().equals(Substation.CATENARY_MATERIAL)) {
				Bukkit.getScheduler().runTaskLater(plugin, new UpdateCatenaryRunnable(), 1L);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if(!event.isCancelled()) {
			Block broken = event.getBlock();
			Player player = event.getPlayer();
			if(broken.getType().equals(Substation.CATENARY_MATERIAL)) {
				Bukkit.getScheduler().runTaskLater(plugin, new UpdateCatenaryRunnable(), 1L);
			}
			if(ObjectUtil.equals(broken.getType(), Substation.SUBSTATION_SUPPORT.toArray()) || broken.getType().equals(Substation.CATENARY_MATERIAL) || broken.getType().equals(Substation.SUBSTATION_BOTTOM) || broken.getType().equals(Substation.SUBSTATION_TOP) || broken.getType().equals(Material.LEVER)) {
				for(Substation substation : plugin.substations.values()) {
					if(substation.isAt(broken.getLocation())) {
						substation.delete();
						plugin.substations.remove(substation.getName());
						event.setCancelled(true);
						player.sendMessage(ChatColor.GOLD + "You removed that substation.");
						Bukkit.getScheduler().runTaskLater(plugin, new UpdateCatenaryRunnable(), 1L);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockRedstone(BlockRedstoneEvent event) {
		Block changed = event.getBlock();
		if(changed.getType().equals(Material.LEVER)) {
			for(Substation substation : plugin.substations.values()) {
				if(substation.isLeverAt(changed.getLocation())) {
					if(event.getNewCurrent() > 0) {
						substation.turnOn();
					} else {
						substation.turnOff();
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryPickupItem(InventoryPickupItemEvent event) {
		if(!event.isCancelled()) {
			InventoryHolder holder = event.getInventory().getHolder();
			if(holder instanceof HopperMinecart) {
				Train train = plugin.getTrain(MinecartGroup.get((HopperMinecart)holder));
				if(train instanceof SteamTrain) {
					Item item = event.getItem();
					if(SteamTrain.isFuel(item.getItemStack().getType())) {
						train.addFuel(item.getItemStack().getAmount() * 1200);
						event.setCancelled(true);
						item.remove();
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onVehicleEnter(VehicleEnterEvent event) {
		if(!event.isCancelled()) {
			Bukkit.getScheduler().runTaskLater(plugin, new VehicleEnterRunnable(event), 10L);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onVehicleExit(VehicleExitEvent event) {
		if(!event.isCancelled()) {
			if(event.getVehicle() instanceof RideableMinecart && event.getExited() instanceof Player) {
				Player player = (Player)event.getExited();
				Train train = plugin.getTrain(player, true);
				if(train != null && train.isLeader(player)) {
					train.setCombinedLever(-4, false);
				}
			}
		}
	}
	
	private class UpdateCatenaryRunnable implements Runnable {
		
		public void run() {
			plugin.updateCatenary();
		}
		
	}
	
	private class VehicleEnterRunnable implements Runnable {
		
		private VehicleEnterEvent event;
		
		private VehicleEnterRunnable(VehicleEnterEvent event) {
			this.event = event;
		}
		
		public void run() {
			if(event.getVehicle() instanceof RideableMinecart && event.getEntered() instanceof Player) {
				Player player = (Player)event.getEntered();
				Train train = plugin.getTrain(player, false);
				if(train != null) {
					plugin.giveControlPanelTo(player, train);
				}
			}
		}
		
	}
	
}