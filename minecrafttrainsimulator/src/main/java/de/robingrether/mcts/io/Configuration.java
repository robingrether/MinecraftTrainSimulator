package de.robingrether.mcts.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import de.robingrether.mcts.MinecraftTrainSimulator;
import de.robingrether.mcts.Substation;
import de.robingrether.util.StringUtil;

public class Configuration {
	
	/* Configuration options start here */
	public static final String UNIT_OF_SPEED_PATH = "status-panel.unit-of-speed";
	public static final String UPDATE_CHECK_PATH = "updates.check";
	public static final String UPDATE_DOWNLOAD_PATH = "updates.download";
	public static final String CATENARY_MATERIAL_PATH = "catenary.material";
	public static final String CATENARY_SUBSTATION_BOTTOM_PATH = "catenary.substation.bottom-block";
	public static final String CATENARY_SUBSTATION_TOP_PATH = "catenary.substation.top-block";
	public static final String CATENARY_SUBSTATION_SUPPORT_PATH = "catenary.substation.support-blocks";
	public static final String CATENARY_HEIGHT_PATH = "catenary.height";
	
	public String UNIT_OF_SPEED = "kmh";
	public boolean UPDATE_CHECK = true;
	public boolean UPDATE_DOWNLOAD = false;
	public String CATENARY_MATERIAL = Substation.CATENARY_MATERIAL.name(); //TODO: add to config.yml
	public String CATENARY_SUBSTATION_BOTTOM = Substation.SUBSTATION_BOTTOM.name();
	public String CATENARY_SUBSTATION_TOP = Substation.SUBSTATION_TOP.name();
	public List<String> CATENARY_SUBSTATION_SUPPORT = Substation.SUBSTATION_SUPPORT.stream().map(Material::name).collect(Collectors.toList());
	public int CATENARY_HEIGHT = Substation.CATENARY_HEIGHT;
	/* Configuration options end here */
	
	
	private MinecraftTrainSimulator plugin;
	
	public Configuration(MinecraftTrainSimulator plugin) {
		this.plugin = plugin;
	}
	
	public void loadData() {
		plugin.reloadConfig();
		FileConfiguration fileConfiguration = plugin.getConfig();
		try {
			for(Field pathField : getClass().getDeclaredFields()) {
				if(pathField.getName().endsWith("_PATH")) {
					Field valueField = getClass().getDeclaredField(pathField.getName().substring(0, pathField.getName().length() - 5));
					if(fileConfiguration.isSet((String)pathField.get(null))) {
						if(fileConfiguration.isString((String)pathField.get(null))) {
							valueField.set(this, fileConfiguration.getString((String)pathField.get(null), (String)valueField.get(this)));
						} else if(fileConfiguration.isBoolean((String)pathField.get(null))) {
							valueField.setBoolean(this, fileConfiguration.getBoolean((String)pathField.get(null), valueField.getBoolean(this)));
						} else if(fileConfiguration.isDouble((String)pathField.get(null))) {
							valueField.setDouble(this, fileConfiguration.getDouble((String)pathField.get(null), valueField.getDouble(this)));
						} else if(fileConfiguration.isInt((String)pathField.get(null))) {
							valueField.setInt(this, fileConfiguration.getInt((String)pathField.get(null), valueField.getInt(this)));
						} else if(fileConfiguration.isList((String)pathField.get(null))) {
							valueField.set(this, fileConfiguration.getList((String)pathField.get(null), (List<String>)valueField.get(this)));
						}
					}
				}
			}
		} catch(Exception e) {
			plugin.getLogger().log(Level.SEVERE, "An error occured while loading the config file.", e);
		}
	}
	
	public void saveData() {
		File configurationFile = new File(plugin.getDataFolder(), "config.yml");
		String config = StringUtil.readFrom(plugin.getResource("config.yml"));
		try {
			for(Field pathField : getClass().getDeclaredFields()) {
				if(pathField.getName().endsWith("_PATH")) {
					Field valueField = getClass().getDeclaredField(pathField.getName().substring(0, pathField.getName().length() - 5));
					if(valueField.getType() == List.class) {
						StringBuilder builder = new StringBuilder();
						String indentation = String.join("", Collections.nCopies((int)((String)pathField.get(null)).chars().filter(c -> c == '.').count() * 3, " "));
						for(Object object : ((List)valueField.get(this))) {
							builder.append("\r\n" + indentation + "- " + object.toString());
						}
						config = config.replace(valueField.getName(), builder.toString());
					} else {
						config = config.replace(valueField.getName(), valueField.get(this).toString());
					}
				}
			}
			OutputStream output = new FileOutputStream(configurationFile);
			output.write(config.getBytes());
			output.close();
		} catch(Exception e) {
			plugin.getLogger().log(Level.SEVERE, "An error occured while saving the config file.", e);
		}
	}
	
}