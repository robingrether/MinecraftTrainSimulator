package de.robingrether.mcts;

import org.bukkit.entity.Minecart;
import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.controller.type.MinecartMemberFurnace;

public class Accelerator extends BukkitRunnable {
	
	private Train train;
	private double targetVelocity;
	private int amplifier;
	private int furnaceCarts = 0;
	
	public Accelerator(Train train, double targetVelocity, int amplifier) {
		this.train = train;
		this.targetVelocity = targetVelocity;
		this.amplifier = amplifier;
		for(MinecartMember<?> minecart : train.getMinecarts()) {
			if(minecart instanceof MinecartMemberFurnace) furnaceCarts++;
			if(furnaceCarts == 4) break;
		}
	}
	
	public double getTargetVelocity() {
		return targetVelocity;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		if(minecarts.isEmpty()) {
			train.terminate();
			return;
		}
		double force = minecarts.getAverageForce();
		if(targetVelocity == 0.0) {
			if(force > 0) {
				force -= 0.0005 * amplifier;
				if(force < 0) {
					force = 0.0;
				}
			} else if(force < 0) {
				force += 0.0005 * amplifier;
				if(force > 0) {
					force = 0.0;
				}
			}
		} else {
			if(!(train.getLeader().getVehicle() instanceof Minecart)) return;
			if(!train.hasFuel()) return;
			boolean facingForward = minecarts.head().equals(MinecartMemberStore.convert((Minecart)train.getLeader().getVehicle()));
			force *= facingForward ? 1.0 : -1.0;
			double targetVelocitySigned = targetVelocity * train.getDirection();
			if(force < targetVelocitySigned) {
				force += targetVelocity * 0.02 * furnaceCarts;
				if(force > targetVelocitySigned) {
					force = targetVelocitySigned;
				}
			} else if(force > targetVelocitySigned) {
				force -= targetVelocity * 0.02 * furnaceCarts;
				if(force < targetVelocitySigned) {
					force = targetVelocitySigned;
				}
			}
			force *= facingForward ? 1.0 : -1.0;
			train.consumeFuel();
		}
		minecarts.setForwardForce(force);
	}
	
}