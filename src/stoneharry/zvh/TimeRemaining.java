package stoneharry.zvh;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TimeRemaining implements Runnable {
	@Override
	public void run() {
		try {
			while (!Main.ShuttingDown) {
				if (Main.gameRunning && !Main.resetting) {
					int result = Main.CheckForTimeUp();
					if (result != 0) {
						// Get time remaining
						int seconds = (int) (Main.timeLimit / 1000)
								- (result / 1000);
						int minutes = 0;
						while (seconds > 59) {
							seconds = seconds - 60;
							minutes = minutes + 1;
						}
						if (minutes == 0) {
							Bukkit.broadcastMessage(ChatColor.RED + Main.prefix
									+ " " + ChatColor.AQUA + "There is "
									+ String.valueOf(seconds)
									+ " seconds remaining!");
						} else {
							Bukkit.broadcastMessage(ChatColor.RED + Main.prefix
									+ " " + ChatColor.AQUA + "There is "
									+ String.valueOf(minutes) + " minutes and "
									+ String.valueOf(seconds)
									+ " seconds remaining!");
						}
						// If mark zombies put lightning on humans
						if (Main.mark_zombies) {
							Bukkit.broadcastMessage(ChatColor.RED
									+ Main.prefix
									+ " "
									+ ChatColor.AQUA
									+ "The lightning shows where the remaining humans are!");
							for (String name : Main.humans.values()) {
								Player player = Bukkit.getPlayer(name);
								if (player != null) {
									if (player.isOnline()) {
										ScoreSystem.incrementTimerScore(player
												.getName());
										Bukkit.getWorld(Main.worldName)
												.strikeLightningEffect(
														player.getLocation());
									}
								}
							}
							Main.mark_zombies = false;
						} else {
							Main.mark_zombies = true;
						}
					}
					// Check for offline humans and remove them if found
					if (!Main.humans.isEmpty()) {
						ArrayList<String> NamesToRemove = new ArrayList<String>();
						for (String name : Main.humans.values()) {
							if (Bukkit.getPlayer(name) == null) {
								NamesToRemove.add(name);
							} else {
								if (!Bukkit.getPlayer(name).isOnline()) {
									NamesToRemove.add(name);
								}
							}
						}
						for (int i = 0; i < NamesToRemove.size(); i++) {
							Main.humans.remove(NamesToRemove.get(i));
						}
						NamesToRemove.clear();
					}
				}
				Thread.sleep(28000);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}