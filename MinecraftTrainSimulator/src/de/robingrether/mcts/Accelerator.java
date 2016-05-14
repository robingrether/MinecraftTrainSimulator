package de.robingrether.mcts;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.controller.type.MinecartMemberFurnace;

public class Accelerator extends TrainThread {
	
	private Train train;
	private double acceleration;
	private boolean execute;
	
	public Accelerator(Train train, double acceleration) {
		this.train = train;
		this.acceleration = acceleration;
		this.execute = true;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		while(execute) {
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
			try {
				sleep(50L);
			} catch(InterruptedException e) {
			}
		}
	}
	
	public void terminate() {
		execute = false;
	}
	
}