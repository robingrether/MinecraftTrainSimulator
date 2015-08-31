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
		boolean accelerate = true;
		while(execute) {
			if(train.getDirection() == 1) {
				if(!minecarts.head().equals(MinecartMemberStore.get(train.getLeader().getVehicle()))) {
					if(minecarts.getAverageForce() < 0.02) {
						minecarts.reverse();
						accelerate = true;
					} else {
						minecarts.stop();
						accelerate = false;
					}
				}
			} else if(train.getDirection() == -1) {
				if(minecarts.head().equals(MinecartMemberStore.get(train.getLeader().getVehicle()))) {
					if(minecarts.getAverageForce() < 0.02) {
						minecarts.reverse();
						accelerate = true;
					} else {
						minecarts.stop();
						accelerate = false;
					}
				}
			}
			if(accelerate && train.getDirection() != 0 && train.hasFuel()) {
				double force = minecarts.getAverageForce();
				if(!(force > maxVelocity)) {
					force += acceleration;
					if(force > maxVelocity) {
						if(force - acceleration > maxVelocity) {
							force -= acceleration;
						} else {
							force = maxVelocity;
						}
					}
				}
				minecarts.setForwardForce(force);
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