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
				force += 0.005 * amplifier * train.getDirection();
				for(MinecartMember<?> minecart : minecarts) {
					if(minecart instanceof MinecartMemberFurnace) {
						minecart.setForwardForce(force * (facingForward ? 1 : -1));
					}
				}
				train.consumeFuel();
			}
		}
	}
	
}