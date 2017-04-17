package de.robingrether.mcts.render;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import static org.bukkit.map.MinecraftFont.Font;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import de.robingrether.mcts.ElectricTrain;
import de.robingrether.mcts.Train;

public class TrainMapRenderer extends MapRenderer {
	
	private static final NumberFormat signedFormatter = new DecimalFormat("+#;-#");
	
	private Train train;
	
	public TrainMapRenderer(Train train) {
		this.train = train;
	}
	
	public void render(MapView map, MapCanvas canvas, Player player) {
		if(train == null) {
			return;
		} else if(train.getMinecarts() == null || train.getMinecarts().size() == 0) {
			return;
		}
		try {
			canvas.drawImage(0, 0, Images.getImage(1));
			canvas.drawImage(4, 19, Images.getImage((train instanceof ElectricTrain ? 4 : 2) + (train.isAccelerating() ? 1 : 0)));
			canvas.drawImage(0, 96, Images.getImage(0));
			canvas.drawText(100, 20, Font, Integer.toString((int)(train.getMinecarts().getAverageForce() * 100)));
			canvas.drawText(100, 35, Font, Integer.toString(train.getMinecarts().head().getBlock(0, -1, 0).getY()));
			canvas.drawText(82, 50, Font, train instanceof ElectricTrain ? (train.hasFuel() ? "yes" : "no") : Integer.toString(train.getFuel()));
			canvas.drawText(14, 50, Font, signedFormatter.format(train.getDirection()));
			canvas.drawText(14, 65, Font, signedFormatter.format(train.getCombinedLever()));
			canvas.drawText(5, 80, Font, train.getMinecarts().getProperties().getDisplayName());
		} catch(NullPointerException e) {
			return;
		}
	}
	
}