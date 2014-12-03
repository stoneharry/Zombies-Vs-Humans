package stoneharry.zvh;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class timeRemaining implements Runnable {
	@Override
	public void run() {
		try {
			while (!main.ShuttingDown) {
				if (main.gameRunning && !main.resetting) {
					int result = main.CheckForTimeUp();
					if (result != 0) {
						// Get time remaining
						int seconds = (int) (main.timeLimit / 1000)
								- (result / 1000);
						int minutes = 0;
						while (seconds > 59) {
							seconds = seconds - 60;
							minutes = minutes + 1;
						}
						if (minutes == 0) {
							Bukkit.broadcastMessage(ChatColor.RED + main.prefix
									+ " " + ChatColor.AQUA + "There is "
									+ String.valueOf(seconds)
									+ " seconds remaining!");
						} else {
							Bukkit.broadcastMessage(ChatColor.RED + main.prefix
									+ " " + ChatColor.AQUA + "There is "
									+ String.valueOf(minutes) + " minutes and "
									+ String.valueOf(seconds)
									+ " seconds remaining!");
						}
						// If mark zombies put lightning on humans
						if (main.mark_zombies) {
							Bukkit.broadcastMessage(ChatColor.RED
									+ main.prefix
									+ " "
									+ ChatColor.AQUA
									+ "The lightning shows where the remaining humans are!");
							for (String name : main.humans.values()) {
								Player player = Bukkit.getPlayer(name);
								if (player != null) {
									if (player.isOnline()) {
										scoreSystem.incrementTimerScore(player
												.getName());
										Bukkit.getWorld(main.worldName)
												.strikeLightningEffect(
														player.getLocation());
									}
								}
							}
							main.mark_zombies = false;
						} else {
							main.mark_zombies = true;
						}
					}
					// Check for offline humans and remove them if found
					if (!main.humans.isEmpty()) {
						ArrayList<String> NamesToRemove = new ArrayList<String>();
						for (String name : main.humans.values()) {
							if (Bukkit.getPlayer(name) == null) {
								NamesToRemove.add(name);
							} else {
								if (!Bukkit.getPlayer(name).isOnline()) {
									NamesToRemove.add(name);
								}
							}
						}
						for (int i = 0; i < NamesToRemove.size(); i++) {
							main.humans.remove(NamesToRemove.get(i));
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