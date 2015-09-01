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
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

import de.robingrether.util.ObjectUtil;

public class MCTSListener implements Listener {
	
	private MinecraftTrainSimulator plugin;
	Map<String, Substation> substations = new ConcurrentHashMap<String, Substation>();
	
	public MCTSListener(MinecraftTrainSimulator plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		if(!event.isCancelled()) {
			Block placed = event.getBlockPlaced();
			Player player = event.getPlayer();
			if(placed.getType().equals(Material.REDSTONE_BLOCK)) {
				if(substations.containsKey(player.getName().toLowerCase(Locale.ENGLISH))) {
					Substation substation = substations.get(player.getName().toLowerCase(Locale.ENGLISH));
					if(!substation.isRedstoneBlockPlaced()) {
						substation.placeRedstoneBlock(placed.getLocation());
						player.sendMessage(ChatColor.GOLD + "Now place a fence (wooden or netherbrick) next to the rail.");
					}
				}
			} else if(ObjectUtil.equals(placed.getType(), Material.FENCE, Material.BIRCH_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE, Material.JUNGLE_FENCE, Material.SPRUCE_FENCE, Material.NETHER_FENCE)) {
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
			} else if(placed.getType().equals(Material.IRON_FENCE)) {
				plugin.updateCatenary();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if(!event.isCancelled()) {
			Block broken = event.getBlock();
			Player player = event.getPlayer();
			if(ObjectUtil.equals(broken.getType(), Material.FENCE, Material.BIRCH_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE, Material.JUNGLE_FENCE, Material.SPRUCE_FENCE, Material.NETHER_FENCE, Material.REDSTONE_BLOCK, Material.IRON_FENCE, Material.IRON_BLOCK, Material.LEVER)) {
				for(Substation substation : plugin.substations.values()) {
					if(substation.isAt(broken.getLocation())) {
						substation.delete();
						plugin.substations.remove(substation.getName());
						event.setCancelled(true);
						player.sendMessage(ChatColor.GOLD + "You removed that substation.");
					}
				}
			}
			if(broken.getType().equals(Material.IRON_FENCE)) {
				plugin.updateCatenary();
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
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new VehicleEnterRunnable(event), 10L);
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
	
	public class VehicleEnterRunnable implements Runnable {
		
		private VehicleEnterEvent event;
		
		private VehicleEnterRunnable(VehicleEnterEvent event) {
			this.event = event;
		}
		
		public void run() {
			if(event.getVehicle() instanceof RideableMinecart && event.getEntered() instanceof Player) {
				Player player = (Player)event.getEntered();
				Train train = plugin.getTrain(player, false);
				if(train != null) {
					PlayerInventory inventory = player.getInventory();
					int slot = inventory.first(Material.MAP);
					if(slot > -1) {
						inventory.getItem(slot).setDurability(train.getMapId());
					}
				}
			}
		}
		
	}
	
}