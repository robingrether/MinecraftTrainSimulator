package de.robingrether.mcts;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

public class MCTSListener implements Listener {
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryPickupItem(InventoryPickupItemEvent event) {
		if(!event.isCancelled()) {
			InventoryHolder holder = event.getInventory().getHolder();
			if(holder instanceof HopperMinecart) {
				Train train = MinecraftTrainSimulator.getInstance().getTrain(MinecartGroup.get((HopperMinecart)holder));
				if(train != null) {
					Item item = event.getItem();
					if(Train.isFuel(item.getItemStack().getType())) {
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
			Bukkit.getScheduler().scheduleSyncDelayedTask(MinecraftTrainSimulator.getInstance(), new VehicleEnterRunnable(event), 10L);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onVehicleExit(VehicleExitEvent event) {
		if(!event.isCancelled()) {
			if(event.getVehicle() instanceof RideableMinecart && event.getExited() instanceof Player) {
				Player player = (Player)event.getExited();
				Train train = MinecraftTrainSimulator.getInstance().getTrain(player, true);
				if(train != null) {
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
				Train train = MinecraftTrainSimulator.getInstance().getTrain(player, false);
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