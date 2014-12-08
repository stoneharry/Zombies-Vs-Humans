package stoneharry.zvh;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class HandleMovement {
	private static Boolean FirstPlayerZombie = false;

	public static void HandleEvent(PlayerMoveEvent event) {
		if (Main.gameRunning) {
			Player plr = event.getPlayer();
			// Get nearby players
			for (Entity entity : plr.getNearbyEntities(0.5, 0.5, 0.5)) {
				if (entity instanceof Player) {
					Player target = (Player) entity;
					// Check to see if the moving person is a zombie and the
					// other a human
					if (Main.humans.contains(target.getName())
							&& !Main.humans.contains(plr.getName())) {
						if (plr.hasLineOfSight(target)) {
							// The person will no longer be a human
							Main.humans.remove(target.getName());
							// Zombie caught human so give him points
							ScoreSystem.zombiePointIncrement(plr.getName());
							// Set the messages and change his name
							Bukkit.broadcastMessage(ChatColor.RED + Main.prefix
									+ " " + ChatColor.AQUA + plr.getName()
									+ " has infected " + target.getName() + "!");
							target.setDisplayName("[" + ChatColor.RED
									+ "Zombie" + ChatColor.WHITE + "] "
									+ target.getName());
							target.getInventory().setHelmet(
									new ItemStack(Material.JACK_O_LANTERN, 1));
							target.updateInventory();
							target.setPlayerListName(ChatColor.RED
									+ target.getName());
							// Check to see if all humans have been found
							if (Main.humans.isEmpty() && Main.gameRunning
									&& !Main.resetting) {
								Bukkit.broadcastMessage(ChatColor.RED
										+ Main.prefix
										+ " "
										+ ChatColor.AQUA
										+ "All humans have been caught! The game will end in 10 seconds...");
								Main.prepareReset();
							} else {
								Bukkit.broadcastMessage(ChatColor.RED
										+ Main.prefix + " " + ChatColor.AQUA
										+ "There are "
										+ String.valueOf(Main.humans.size())
										+ " humans remaining!");
							}
						}
					}
				}
			}
		} else {
			// Check to start the game
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			if (players.size() > 1) {
				// Set the time the round started
				Main.lastTime = System.currentTimeMillis();

				// A not very random, random number generator
				Random ran = new Random();
				int x = ran.nextInt(players.size() - 1);
				if (players.size() == 2) {
					if (FirstPlayerZombie) {
						FirstPlayerZombie = false;
						x = 0;
					} else {
						FirstPlayerZombie = true;
						x = 1;
					}
				}
				Iterator<? extends Player> it = players.iterator();
				int i = -1;
				while (it.hasNext()) {
					++i;
					Player player = it.next();
					// Reset inventory
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					// Add items
					player.getInventory().addItem(
							new ItemStack(Material.DIRT, 20));
					if (i == x) {
						player.getInventory().setHelmet(
								new ItemStack(Material.JACK_O_LANTERN, 1));
					}
					player.updateInventory();
					// If random number = player in online players, set to
					// zombie
					if (i == x) {
						Bukkit.broadcastMessage(ChatColor.RED + Main.prefix
								+ " " + ChatColor.AQUA
								+ "The game has begun and " + player.getName()
								+ " is the first zombie!");
						player.setDisplayName("[" + ChatColor.RED + "Zombie"
								+ ChatColor.WHITE + "] " + player.getName());
						player.setPlayerListName(ChatColor.RED
								+ player.getName());
						HandleRounds.handleTeleport(player, false);
					}
					// Set to human
					else {
						player.setDisplayName("[" + ChatColor.GREEN + "Human"
								+ ChatColor.WHITE + "] " + player.getName());
						player.setPlayerListName(ChatColor.GREEN
								+ player.getName());
						Main.humans.add(player.getName());
						HandleRounds.handleTeleport(player, true);
					}
				}
				// Set global variables
				Main.gameRunning = true;
				Main.resetting = false;
			}
		}
	}
}