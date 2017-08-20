package de.robingrether.mcts;

import org.bukkit.Effect;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

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
	private Accelerator accelerator;
	protected int status;
	private MapView controlPanel;
	private double maxSpeed = 100.0;
	
	protected Train(MinecartGroup minecarts, MapView controlPanel) {
		this.minecarts = minecarts;
		this.leader = null;
		this.direction = 0;
		this.accelerator = null;
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
		if(player.getVehicle() instanceof Minecart) {
			MinecartMember<?> minecart = MinecartMemberStore.convert((Minecart)player.getVehicle());
			if(minecarts.head().equals(minecart)) {
				this.leader = player;
				return true;
			} else if(minecarts.tail().equals(minecart)) {
				this.leader = player;
				return true;
			}
		}
		return false;
	}
	
	public boolean canLead(Player player) {
		if(player.getVehicle() instanceof Minecart) {
			MinecartMember<?> minecart = MinecartMemberStore.convert((Minecart)player.getVehicle());
			if(minecarts.head().equals(minecart) || minecarts.tail().equals(minecart)) {
				 return true;
			}
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
			accelerator = new Accelerator(this, getSpeedLimit() * status / 4.0, status);
			if(leader != null && playEffect) {
				leader.getWorld().playEffect(leader.getLocation(), Effect.DOOR_TOGGLE, 0);
			}
			accelerator.runTaskTimer(MinecraftTrainSimulator.getInstance(), 1L, 1L);
		} else if(status < 0) {
			accelerator = new Accelerator(this, 0.0, -status);
			if(leader != null && playEffect) {
				leader.getWorld().playEffect(leader.getLocation(), Effect.DOOR_TOGGLE, 0);
			}
			accelerator.runTaskTimer(MinecraftTrainSimulator.getInstance(), 1L, 1L);
		}
		this.status = status;
	}
	
	public abstract boolean consumeFuel();
	
	public abstract boolean hasFuel();
	
	public double getSpeedLimit() {
		return maxSpeed;
	}
	
	public boolean isAccelerating() {
		return accelerator.getTargetVelocity() != 0.0 && hasFuel();
	}
	
	public void terminate() {
		if(accelerator != null) {
			accelerator.cancel();
		}
	}
	
	public short getMapId() {
		return controlPanel.getId();
	}
	
	private void initTrainProperties() {
		TrainProperties properties = minecarts.getProperties();
		properties.playerCollision = CollisionMode.PUSH;
		properties.miscCollision = CollisionMode.PUSH;
		properties.trainCollision = CollisionMode.PUSH;
		for(MinecartMember<?> minecart : minecarts) {
			double maxSpeed = minecart.getEntity().getMaxSpeed();
			if(maxSpeed < this.maxSpeed) {
				this.maxSpeed = maxSpeed;
			}
		}
	}
	
	private void initMap() {
		for(MapRenderer renderer : controlPanel.getRenderers()) {
			controlPanel.removeRenderer(renderer);
		}
		controlPanel.addRenderer(new TrainMapRenderer(this));
	}
	
}