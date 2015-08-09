package de.robingrether.mcts;

import java.util.HashSet;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import com.bergerkiller.bukkit.tc.CollisionMode;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import de.robingrether.mcts.render.TrainMapRenderer;

public class Train {
	
	private static final HashSet<Material> fuels = new HashSet<Material>();
	private MinecartGroup minecarts;
	private Player leader;
	private int fuel;
	private int direction;
	private TrainThread thread;
	private int status;
	private MapView map;
	
	public Train(MinecartGroup minecarts, MapView map) {
		this.minecarts = minecarts;
		this.leader = null;
		this.fuel = 0;
		this.direction = 0;
		this.thread = null;
		this.status = 0;
		this.map = map;
		initTrainProperties();
		initMap();
	}
	
	public MinecartGroup getMinecarts() {
		return minecarts;
	}
	
	public void addFuel(int fuel) {
		if(fuel > 0) {
			this.fuel += fuel;
		}
	}
	
	public int getFuel() {
		return fuel;
	}
	
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
	
	public void consumeFuel() {
		if(fuel > 0) {
			fuel--;
		}
	}
	
	public boolean hasFuel() {
		return fuel > 0;
	}
	
	public void terminate() {
		if(thread != null) {
			thread.terminate();
		}
	}
	
	@Deprecated
	public short getMapId() {
		return map.getId();
	}
	
	private void initTrainProperties() {
		TrainProperties properties = minecarts.getProperties();
		properties.mobCollision = CollisionMode.PUSH;
		properties.playerCollision = CollisionMode.PUSH;
		properties.miscCollision = CollisionMode.PUSH;
		properties.trainCollision = CollisionMode.PUSH;
		properties.setSpeedLimit(1.0);
	}
	
	private void initMap() {
		for(MapRenderer renderer : map.getRenderers()) {
			map.removeRenderer(renderer);
		}
		map.addRenderer(new TrainMapRenderer(this));
	}
	
	public static boolean isFuel(Material material) {
		return fuels.contains(material);
	}
	
	public static boolean newFuel(Material material) {
		return fuels.add(material);
	}
	
	static {
		newFuel(Material.COAL);
	}
	
}