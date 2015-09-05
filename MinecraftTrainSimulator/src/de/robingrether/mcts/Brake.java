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
		while(execute) {
			double force = Math.abs(minecarts.getAverageForce());
			force -= braking;
			minecarts.setForwardForce(force * (minecarts.getAverageForce() > 0 ? 1 : minecarts.getAverageForce() < 0 ? -1 : 0));
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