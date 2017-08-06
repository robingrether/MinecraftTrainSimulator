package de.robingrether.mcts;

import org.bukkit.entity.Minecart;
import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.controller.type.MinecartMemberFurnace;

public class Accelerator extends BukkitRunnable {
	
	private Train train;
	private int amplifier;
	
	public Accelerator(Train train, int amplifier) {
		this.train = train;
		this.amplifier = amplifier;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		if(minecarts.isEmpty()) {
			train.terminate();
			return;
		}
		if(!(train.getLeader().getVehicle() instanceof Minecart)) return;
		boolean facingForward = minecarts.head().equals(MinecartMemberStore.convert((Minecart)train.getLeader().getVehicle()));
		if(train.hasFuel()) {
			double force = minecarts.getAverageForce() * (facingForward ? 1 : -1);
			if(force < train.getSpeedLimit() * amplifier / 4.0)	{
				int furnaceCarts = 0;
				for(MinecartMember<?> minecart : minecarts) {
					if(minecart instanceof MinecartMemberFurnace) furnaceCarts++;
				}
				force += train.getSpeedLimit() * 0.01 * amplifier * train.getDirection() * (furnaceCarts < 4 ? furnaceCarts / 4.0 : 1.0);
			} else {
				force = train.getSpeedLimit() * amplifier / 4.0;
			}
			minecarts.setForwardForce(force * (facingForward ? 1 : -1));
			train.consumeFuel();
		}
	}
	
}