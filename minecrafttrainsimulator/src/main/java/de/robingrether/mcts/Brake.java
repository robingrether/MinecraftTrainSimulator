package de.robingrether.mcts;

import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

public class Brake extends BukkitRunnable {
	
	private Train train;
	private int amplifier;
	
	public Brake(Train train, int amplifier) {
		this.train = train;
		this.amplifier = amplifier;
	}
	
	public void run() {
		MinecartGroup minecarts = train.getMinecarts();
		if(minecarts.isEmpty()) {
			train.terminate();
			return;
		}
		double force = Math.abs(minecarts.getAverageForce());
		force -= 0.001 * amplifier;
		minecarts.setForwardForce(force * (minecarts.getAverageForce() > 0 ? 1 : minecarts.getAverageForce() < 0 ? -1 : 0));
	}
	
}