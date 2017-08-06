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
	
	private static final NumberFormat signedFormat = new DecimalFormat("+#;-#");
	private static final NumberFormat speedFormat = new DecimalFormat("##0.0 ");
	public static UnitOfSpeed unitOfSpeed = UnitOfSpeed.KILOMETRES_PER_HOUR;
	
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
		String text;
		try {
			canvas.drawImage(0, 0, Images.getImage(0));
			canvas.drawImage(4, 19, Images.getImage((train instanceof ElectricTrain ? 3 : 1) + (train.isAccelerating() ? 1 : 0)));
			
//			canvas.drawText(44, 8, Font, "§16;MCTS");
//			canvas.drawText(84 - Font.getWidth("2.1"), 8, Font, "§16;2.1");
			
//			canvas.drawText(123 - Font.getWidth("Speed"), 51, Font, "§16;Speed");
			text = speedFormat.format(unitOfSpeed.convert(train.getMinecarts().getAverageForce())) + unitOfSpeed.getSymbol();
			canvas.drawText(123 - Font.getWidth(text), 61, Font, text);
			
//			canvas.drawText(123 - Font.getWidth("Height"), 25, Font, "§16;Height");
			text = Integer.toString(train.getMinecarts().head().getBlock(0, -1, 0).getY());
			canvas.drawText(123 - Font.getWidth(text), 35, Font, text);
			
//			canvas.drawText(123 - Font.getWidth("Fuel"), 77, Font, "§16;Fuel");
			text = train instanceof ElectricTrain ? (train.hasFuel() ? "yes" : "no") : Integer.toString(train.getFuel());
			canvas.drawText(123 - Font.getWidth(text), 87, Font, text);
			
//			canvas.drawText(5, 51, Font, "§16;Direction");
			canvas.drawText(5, 61, Font, signedFormat.format(train.getDirection()));
			
//			canvas.drawText(5, 77, Font, "§16;Lever");
			canvas.drawText(5, 87, Font, signedFormat.format(train.getCombinedLever()));
			
//			canvas.drawText(5, 103, Font, "§16;Name");
			canvas.drawText(5, 113, Font, train.getMinecarts().getProperties().getDisplayName());
		} catch(NullPointerException e) {
			return;
		}
	}
	
}