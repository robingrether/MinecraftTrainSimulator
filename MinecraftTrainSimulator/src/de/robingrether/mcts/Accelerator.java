package de.robingrether.mcts;

import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.controller.type.MinecartMemberFurnace;

public class Accelerator extends BukkitRunnable {
	
	private Train train;
	private double acceleration;
	
	public Accelerator(Train train, double acceleration) {
		this.train = train;
		this.acceleration = acceleration;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		boolean facingForward = minecarts.head().equals(MinecartMemberStore.get(train.getLeader().getVehicle()));
		if(train.hasFuel()) {
			double force = minecarts.getAverageForce() * (facingForward ? 1 : -1);
			force += acceleration * train.getDirection();
			for(MinecartMember<?> minecart : minecarts) {
				if(minecart instanceof MinecartMemberFurnace) {
					minecart.setForwardForce(force * (facingForward ? 1 : -1));
				}
			}
			train.consumeFuel();
		}
	}
	
}