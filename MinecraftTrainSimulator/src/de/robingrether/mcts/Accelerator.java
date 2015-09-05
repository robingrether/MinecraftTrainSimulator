package de.robingrether.mcts;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;

public class Accelerator extends TrainThread {
	
	private Train train;
	private double maxVelocity;
	private double acceleration;
	private boolean execute;
	
	public Accelerator(Train train, double maxVelocity, double acceleration) {
		this.train = train;
		this.maxVelocity = maxVelocity;
		this.acceleration = acceleration;
		this.execute = true;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		while(execute) {
			boolean facingForward = minecarts.head().equals(MinecartMemberStore.get(train.getLeader().getVehicle()));
			if(train.hasFuel()) {
				double force = minecarts.getAverageForce();
				if(!(Math.abs(force) > maxVelocity)) {
					force += acceleration * train.getDirection() * (facingForward ? 1 : -1);
					if(Math.abs(force) > maxVelocity) {
						force = maxVelocity * train.getDirection() * (facingForward ? 1 : -1);
					}
				}
				minecarts.setForwardForce(force);
				minecarts.shareForce();
				train.consumeFuel();
			}
			try {
				sleep(100L);
			} catch(InterruptedException e) {
			}
		}
	}
	
	public void terminate() {
		execute = false;
	}
	
}