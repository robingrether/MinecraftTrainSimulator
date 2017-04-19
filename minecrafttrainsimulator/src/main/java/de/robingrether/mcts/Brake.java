package de.robingrether.mcts;

import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

public class Brake extends BukkitRunnable {
	
	private Train train;
	private double braking;
	
	public Brake(Train train, double braking) {
		this.train = train;
		this.braking = braking;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		if(minecarts.isEmpty()) {
			train.terminate();
			return;
		}
		double force = Math.abs(minecarts.getAverageForce());
		force -= braking;
		minecarts.setForwardForce(force * (minecarts.getAverageForce() > 0 ? 1 : minecarts.getAverageForce() < 0 ? -1 : 0));
	}
	
}