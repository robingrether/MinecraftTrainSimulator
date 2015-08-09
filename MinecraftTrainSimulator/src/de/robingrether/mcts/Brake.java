package de.robingrether.mcts;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

public class Brake extends TrainThread {
	
	private Train train;
	private double braking;
	private boolean execute;
	
	public Brake(Train train, double braking) {
		this.train = train;
		this.braking = braking;
		this.execute = true;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		boolean addFuel = false;
		while(execute) {
			double force = minecarts.getAverageForce();
			force -= braking;
			if(force < 0.0) {
				force = 0.0;
			}
			minecarts.setForwardForce(force);
			if(force > 0.0) {
				if(addFuel) {
					train.addFuel(1);
				}
				addFuel = !addFuel;
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