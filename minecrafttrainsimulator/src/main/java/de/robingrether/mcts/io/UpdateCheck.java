package de.robingrether.mcts.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.gson.Gson;

import de.robingrether.mcts.MinecraftTrainSimulator;
import de.robingrether.util.ObjectUtil;
import de.robingrether.util.StringUtil;

public class UpdateCheck implements Runnable {
	
	public static final int PROJECT_ID = 64141;
	public static final String API_URL = "https://api.curseforge.com/servermods/files?projectIds=";
	
	public static class PluginVersion {
		public String md5, downloadUrl, fileName, gameVersion, name, releaseType;
	}
	
	private MinecraftTrainSimulator plugin;
	private String pluginVersion;
	private String latestVersion;
	private CommandSender toBeNotified;
	private String downloadUrl;
	private String checksum;
	private boolean autoDownload;
	
	public UpdateCheck(MinecraftTrainSimulator plugin, CommandSender toBeNotified, boolean autoDownload) {
		this.plugin = plugin;
		this.pluginVersion = plugin.getFullName();
		this.toBeNotified = toBeNotified;
		this.autoDownload = autoDownload;
	}
	
	public void run() {
		checkForUpdate();
		if(isUpdateAvailable()) {
			toBeNotified.sendMessage(ChatColor.GOLD + "[MCTS] An update is available: " + latestVersion);
			if(autoDownload) {
				downloadUpdate();
			} else {
				toBeNotified.sendMessage(ChatColor.GOLD + "[MCTS] You can enable automatic updates in the config file.");
			}
		}
	}
	
	private boolean isUpdateAvailable() {
		if(latestVersion != null && !pluginVersion.equals(latestVersion)) {
			try {
				int current = Integer.parseInt(pluginVersion.split(" |-")[1].replace(".", "")) - (pluginVersion.endsWith("-SNAPSHOT") ? 1 : 0);
				int latest = Integer.parseInt(latestVersion.split(" ")[1].replace(".", ""));
				return latest > current;
			} catch(NumberFormatException e) {
			} catch(ArrayIndexOutOfBoundsException e) {
			}
		}
		return false;
	}
	
	private void checkForUpdate() {
		BufferedReader reader = null;
		try {
			URL url = new URL(API_URL + PROJECT_ID);
			URLConnection connection = url.openConnection();
			connection.addRequestProperty("User-Agent", pluginVersion.replace(' ', '/') + " (by RobinGrether)");
			connection.setDoOutput(true);
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String response = reader.readLine();
			PluginVersion[] array = new Gson().fromJson(response, PluginVersion[].class);
			latestVersion = null;
			int latestVersionNumber = 0;
			for(PluginVersion pluginVersion : array) {
				int versionNumber = extractVersionNumber(pluginVersion.name);
				if(versionNumber > latestVersionNumber) {
					latestVersionNumber = versionNumber;
					latestVersion = pluginVersion.name;
					downloadUrl = pluginVersion.downloadUrl;
					checksum = pluginVersion.md5;
				}
			}
		} catch(Exception e) {
			plugin.getLogger().log(Level.WARNING, "Update checking failed: " + e.getClass().getSimpleName());
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
				}
			}
		}
	}
	
	private void downloadUpdate() {
		File oldFile = plugin.getPluginFile();
		File newFile = new File(plugin.getServer().getUpdateFolderFile(), oldFile.getName());
		if(newFile.exists()) {
			toBeNotified.sendMessage(ChatColor.GOLD + "[MCTS] Update already downloaded. (Restart server to apply update)");
		} else {
			InputStream input = null;
			OutputStream output = null;
			try {
				URL url = new URL(downloadUrl);
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				connection.addRequestProperty("User-Agent", pluginVersion.replace(' ', '/') + " (by RobinGrether)");
				connection.setDoOutput(true);
				if(ObjectUtil.equals(connection.getResponseCode(), 301, 302)) {
					downloadUrl = connection.getHeaderField("Location");
					downloadUpdate();
					return;
				} else if(connection.getResponseCode() != 200) {
					toBeNotified.sendMessage(ChatColor.RED + "[MCTS] Download failed.");
					plugin.getLogger().log(Level.WARNING, "Update download failed: HTTP error");
					return;
				}
				toBeNotified.sendMessage(ChatColor.GOLD + "[MCTS] Downloading update...");
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");
				input = new DigestInputStream(connection.getInputStream(), messageDigest);
				plugin.getServer().getUpdateFolderFile().mkdir();
				output = new FileOutputStream(newFile);
				int fetched;
				byte[] data = new byte[4096];
				while((fetched = input.read(data)) > 0) {
					output.write(data, 0, fetched);
				}
				input.close();
				output.close();
				if(StringUtil.bytesToHex(messageDigest.digest()).equalsIgnoreCase(this.checksum)) {
					toBeNotified.sendMessage(ChatColor.GOLD + "[MCTS] Download succeeded. (Restart server to apply update)");
				} else {
					newFile.delete();
					toBeNotified.sendMessage(ChatColor.RED + "[MCTS] Download failed.");
					plugin.getLogger().log(Level.WARNING, "Update download failed: checksum is bad");
				}
			} catch(IOException | NoSuchAlgorithmException e) {
				toBeNotified.sendMessage(ChatColor.RED + "[MCTS] Download failed.");
				plugin.getLogger().log(Level.WARNING, "Update download failed: " + e.getClass().getSimpleName());
			} finally {
				if(input != null) {
					try {
						input.close();
					} catch(IOException e) {
					}
				}
				if(output != null) {
					try {
						output.close();
					} catch(IOException e) {
					}
				}
			}
		}
	}
	
	public static int extractVersionNumber(String versionString) {
		try {
			String[] numbers = versionString.split(" |-")[1].split("\\.");
			int versionNumber = 0;
			for(int i = 0; i < numbers.length; i++) {
				if(numbers[i].length() > 2)
					return 0;
				
				versionNumber += Integer.parseInt(numbers[i]) * Math.pow(10.0, 2 * (numbers.length - i - 1));
			}
			if(versionString.contains("SNAPSHOT"))
				versionNumber--;
			return versionNumber;
		} catch(ArrayIndexOutOfBoundsException|NumberFormatException e) {
			return 0;
		}
	}
	
}