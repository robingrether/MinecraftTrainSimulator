package de.robingrether.mcts;

import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.bergerkiller.bukkit.tc.CollisionMode;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;

import de.robingrether.mcts.render.TrainMapRenderer;

public abstract class Train {
	
	protected MinecartGroup minecarts;
	private Player leader;
	private int direction;
	private TrainThread thread;
	protected int status;
	private MapView controlPanel;
	
	protected Train(MinecartGroup minecarts, MapView controlPanel) {
		this.minecarts = minecarts;
		this.leader = null;
		this.direction = 0;
		this.thread = null;
		this.status = 0;
		this.controlPanel = controlPanel;
		initTrainProperties();
		initMap();
	}
	
	public MinecartGroup getMinecarts() {
		return minecarts;
	}
	
	public abstract boolean addFuel(int fuel);
	
	public abstract int getFuel();
	
	public Player getLeader() {
		return leader;
	}
	
	public boolean setLeader(Player player) {
		MinecartMember<?> minecart = MinecartMemberStore.get(player.getVehicle());
		if(minecarts.head().equals(minecart)) {
			this.leader = player;
			return true;
		} else if(minecarts.tail().equals(minecart)) {
			this.leader = player;
			if(minecarts.getAverageForce() < 0.02) {
				minecarts.reverse();
			} else {
				minecarts.stop();
				minecarts.reverse();
			}
			return true;
		}
		return false;
	}
	
	public boolean canLead(Player player) {
		MinecartMember<?> minecart = MinecartMemberStore.get(player.getVehicle());
		if(minecarts.head().equals(minecart) || minecarts.tail().equals(minecart)) {
			 return true;
		}
		return false;
	}
	
	public boolean isLeader(Player player) {
		if(leader == null) {
			return false;
		} else {
			return leader.equals(player);
		}
	}
	
	public int getDirection() {
		return direction;
	}
	
	public void setDirection(int direction, boolean playEffect) {
		if(this.direction != direction) {
			this.direction = direction;
			if(leader != null && playEffect) {
				leader.getWorld().playEffect(leader.getLocation(), Effect.DOOR_TOGGLE, 0);
			}
		}
	}
	
	public int getCombinedLever() {
		return status;
	}
	
	public void setCombinedLever(int status, boolean playEffect) {
		if(this.status == status) {
			return;
		}
		terminate();
		if(status > 0) {
			double maxVelocity = 0.0;
			double acceleration = 0.0;
			switch(status) {
				case 1:
					maxVelocity = 0.2;
					acceleration = 0.01;
					break;
				case 2:
					maxVelocity = 0.4;
					acceleration = 0.02;
					break;
				case 3:
					maxVelocity = 0.7;
					acceleration = 0.035;
					break;
				case 4:
					maxVelocity = 1.0;
					acceleration= 0.05;
					break;
			}
			if(maxVelocity > 0 && acceleration > 0) {
				thread = new Accelerator(this, maxVelocity, acceleration);
				if(leader != null && playEffect) {
					leader.getWorld().playEffect(leader.getLocation(), Effect.DOOR_TOGGLE, 0);
				}
				thread.start();
			}
		} else if(status < 0) {
			double braking = 0.0;
			switch(status) {
				case -1:
					braking = 0.001;
					break;
				case -2:
					braking = 0.002;
					break;
				case -3:
					braking = 0.0035;
					break;
				case -4:
					braking = 0.005;
					break;
			}
			if(braking > 0) {
				thread = new Brake(this, braking);
				if(leader != null && playEffect) {
					leader.getWorld().playEffect(leader.getLocation(), Effect.DOOR_TOGGLE, 0);
				}
				thread.start();
			}
		}
		this.status = status;
	}
	
	public abstract boolean consumeFuel();
	
	public abstract boolean hasFuel();
	
	public boolean isAccelerating() {
		return thread instanceof Accelerator && hasFuel();
	}
	
	public void terminate() {
		if(thread != null) {
			thread.terminate();
		}
	}
	
	@Deprecated
	public short getMapId() {
		return controlPanel.getId();
	}
	
	private void initTrainProperties() {
		TrainProperties properties = minecarts.getProperties();
		properties.playerCollision = CollisionMode.PUSH;
		properties.miscCollision = CollisionMode.PUSH;
		properties.trainCollision = CollisionMode.PUSH;
		properties.setSpeedLimit(1.0);
	}
	
	private void initMap() {
		for(MapRenderer renderer : controlPanel.getRenderers()) {
			controlPanel.removeRenderer(renderer);
		}
		controlPanel.addRenderer(new TrainMapRenderer(this));
	}
	
}