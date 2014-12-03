package stoneharry.zvh;

import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class main extends JavaPlugin implements Listener {
	public static main instance = null;
	public static Logger console = null;
	public static HashMap<String, String> humans;
	public static Boolean gameRunning = false;
	public static Boolean resetting = true;
	public static Stack<Object> blocks_changed = new Stack<Object>();
	public static long timeLimit = 300000;
	public static long lastTime = 0;
	// Mark zombies is used to decide whether to mark humans with lightning or
	// not
	public static Boolean mark_zombies = true;
	// This handles which round should be started
	public static int round = 1;
	public static int numRounds = 1;
	public static Boolean ShuttingDown = false;
	public static String prefix = "[Server]";
	public static Boolean useScoreSystem = true;
	public static String SQLAddress = "127.0.0.1";
	public static String SQLUsername = "root";
	public static String SQLPassword = "root";
	public static String SQLPort = "3306";
	public static String ZombiesDB = "minecraft";
	public static Boolean useAuthMe = false;
	public static String AuthMeDB = "authme";
	public static String worldName = "zombies";
	public static Location[] RoundHumanLocations;
	public static Location[] RoundZombieLocations;
	public static int pillarMaxHeight = 2;
	private static ScoreboardManager manager = null;
	private static Scoreboard board = null;
	public static Objective objective = null;

	private ConsoleCommandSender commandConsole = Bukkit.getServer()
			.getConsoleSender();

	private void LoadConfig() {
		// The following method will not overwrite an existing file.
		saveDefaultConfig();
		timeLimit = getConfig().getInt("timeLimit");
		prefix = getConfig().getString("ServerName");
		useScoreSystem = getConfig().getBoolean("UseScoreSystem");
		SQLAddress = getConfig().getString("SQLAddress");
		SQLUsername = getConfig().getString("SQLUsername");
		SQLPassword = getConfig().getString("SQLPassword");
		SQLPort = getConfig().getString("SQLPassword");
		ZombiesDB = getConfig().getString("ZombiesDatabase");
		useAuthMe = getConfig().getBoolean("useAuthMe");
		AuthMeDB = getConfig().getString("AuthMeDatabase");
		numRounds = getConfig().getInt("NumArenas");
		worldName = getConfig().getString("WorldName");

		RoundHumanLocations = new Location[numRounds];
		RoundZombieLocations = new Location[numRounds];
		for (int i = 0; i < numRounds; i++) {
			int x = 0;
			int y = 0;
			int z = 0;
			x = getConfig().getInt("Human" + String.valueOf(i + 1) + "x");
			y = getConfig().getInt("Human" + String.valueOf(i + 1) + "y");
			z = getConfig().getInt("Human" + String.valueOf(i + 1) + "z");
			RoundHumanLocations[i] = new Location(Bukkit.getServer().getWorld(
					worldName), x, y, z);
			x = getConfig().getInt("Zombie" + String.valueOf(i + 1) + "x");
			y = getConfig().getInt("Zombie" + String.valueOf(i + 1) + "y");
			z = getConfig().getInt("Zombie" + String.valueOf(i + 1) + "z");
			RoundZombieLocations[i] = new Location(Bukkit.getServer().getWorld(
					worldName), x, y, z);
		}
	}

	@Override
	public void onDisable() {
		// Dispose of possible large data and try to rollback
		try {
			scoreSystem.DropConnection();
			resetLevelNow();
			blocks_changed.clear();
			humans.clear();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			ShuttingDown = true;
		}
		commandConsole.sendMessage(ChatColor.AQUA + "########################");
		commandConsole.sendMessage(ChatColor.AQUA + "[StonedZombie] "
				+ ChatColor.RED + " Disabled!");
		commandConsole.sendMessage(ChatColor.AQUA + "########################");
	}

	@Override
	public void onEnable() {
		// Set up variables
		manager = Bukkit.getScoreboardManager();
		board = manager.getNewScoreboard();
		objective = board.registerNewObjective("board", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.AQUA + "High Scores");
		instance = this;
		console = Logger.getLogger("Minecraft");
		humans = new HashMap<String, String>();
		commandConsole = Bukkit.getServer().getConsoleSender();
		// Register events
		getServer().getPluginManager().registerEvents(this, this);
		// Load the config
		LoadConfig();
		// Get MySQL connection
		if (scoreSystem.EstablishConnection()) {
			// Check for time up and lightning every 28 seconds
			new Thread(new timeRemaining()).start();
			// This thread gets a accurate game ending
			new Thread(new gameEndCheck()).start();
			// Annoying message
			commandConsole.sendMessage(ChatColor.AQUA
					+ "########################");
			commandConsole.sendMessage(ChatColor.AQUA + "[StonedZombie] "
					+ ChatColor.RED + " Enabled!");
			commandConsole.sendMessage(ChatColor.AQUA
					+ "########################");
		} else {
			// No point starting up
			commandConsole.sendMessage(ChatColor.RED
					+ "########################");
			commandConsole.sendMessage(ChatColor.AQUA
					+ "ERROR : MySQL connection could not be established.");
			commandConsole.sendMessage(ChatColor.RED
					+ "########################");
			Bukkit.shutdown();
		}
	}

	public static int CheckForTimeUp() {
		// Get time elapsed
		long result = System.currentTimeMillis() - lastTime;
		// See if the time elapsed is more than the time limit
		if (result > timeLimit) {
			for (String name : humans.values())
				scoreSystem.incrementSurvivorScore(name);
			Bukkit.broadcastMessage(ChatColor.RED
					+ prefix
					+ " "
					+ ChatColor.AQUA
					+ "The time limit is up, humans have won! The game will end in 10 seconds...");
			prepareReset();
			return 0;
		}
		return (int) result;
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		try {
			handleMovement.HandleEvent(event);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// This function just sets the variable to show the round has ended but it
	// is waiting 10 seconds
	public static void prepareReset() {
		resetting = true;
		humans.clear();
		instance.resetLevel();
	}

	public void resetLevel() {
		new BukkitRunnable() {
			@Override
			public void run() {
				resetLevelNow();
			}

		}.runTaskLater(this, 20 * 10);
	}

	@SuppressWarnings("deprecation")
	private static void resetLevelNow() {
		// Reset the map by rolling back changes
		while (!main.blocks_changed.isEmpty()) {
			Byte b;
			Material mat;
			Location loc;
			if (main.blocks_changed.size() < 3) {
				main.console.info("ERROR: Stack size was less than 3.");
				main.blocks_changed.clear();
				continue;
			}
			Object aa = main.blocks_changed.pop();
			Object bb = main.blocks_changed.pop();
			Object cc = main.blocks_changed.pop();
			if (aa instanceof Byte) {
				b = (Byte) aa;
			} else {
				main.console
						.info("ERROR: First pop is not a instance of a Byte.");
				continue;
			}
			if (bb instanceof Material) {
				mat = (Material) bb;
			} else {
				main.console
						.info("ERROR: Second pop is not a instance of a Integer.");
				continue;
			}
			if (cc instanceof Location) {
				loc = (Location) cc;
			} else {
				main.console
						.info("ERROR: Third pop is not a instance of a Location.");
				continue;
			}

			loc.getBlock().setTypeIdAndData(mat.getId(), b, true);
		}

		for (World world : Bukkit.getServer().getWorlds()) {
			for (Entity e : world.getEntities()) {
				if (e instanceof Item) {
					e.remove();
				}
			}
		}

		// Make sure the needed variables change
		main.gameRunning = false;
		main.resetting = false;
		main.mark_zombies = false;
		handleRounds.incrementRound();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		// When a player joins, set them to zombie and update them if game is
		// running, teleport.
		Player p = event.getPlayer();
		if (p != null) {
			if (p.isOnline()) {
				handleRounds.handleTeleport(p, false);
				scoreSystem.addPlayerToScoreIfNotExists(p.getName());
				p.sendMessage(ChatColor.RED
						+ prefix
						+ " "
						+ ChatColor.AQUA
						+ "You can view the top scores with: /score. You can view your personal score with: /myscore.");
				if (gameRunning) {
					p.sendMessage(ChatColor.RED
							+ prefix
							+ " "
							+ ChatColor.AQUA
							+ "The game is in progress already - you have joined as a Zombie. You must infect as many humans as you can, just run into them!");
					// Bukkit.broadcastMessage(ChatColor.RED + prefix + " " +
					// ChatColor.AQUA + p.getName() +
					// " has joined the Zombies!");
					p.setDisplayName("[" + ChatColor.RED + "Zombie"
							+ ChatColor.WHITE + "] " + p.getName());
					p.setPlayerListName(ChatColor.RED + p.getName());
					p.getInventory().addItem(new ItemStack(Material.DIRT, 20));
					p.getInventory().setHelmet(
							new ItemStack(Material.JACK_O_LANTERN, 1));
					p.updateInventory();
					if (humans.containsKey(p.getName())) {
						humans.remove(p.getName());
					}
				}
			}
		}
		event.setJoinMessage(null);
		Score s = objective.getScore(p.getName());
		s.setScore(0);
		p.setScoreboard(board);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player p = event.getPlayer();
		if (p.isOnline()) {
			event.disallow(
					PlayerLoginEvent.Result.KICK_OTHER,
					"That username is already online. Don't try to disconnect people - you will be ban.");
			return;
		}
		if (p.getName().contains("?")) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
					"Question marks are not allowed in usernames.");
			return;
		}
		if (p.getName().equals("Player")) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
					"The username 'Player' is not allowed. Are you logged in?");
		}
	}

	// Prevents leave server message being spammed
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		event.setQuitMessage(null);
		board.resetScores(event.getPlayer().getName());
	}

	// Make players immune to damage
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		event.setCancelled(true);
	}

	// Prevent players getting hungry
	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public synchronized void onBlockPlace(BlockPlaceEvent event) {
		// If game is in progress
		if (!resetting) {
			Material replaced = event.getBlockReplacedState().getType();
			if (replaced == Material.WATER
					|| replaced == Material.STATIONARY_WATER
					|| replaced == Material.LAVA) {
				event.getPlayer().sendMessage(
						ChatColor.RED
								+ "You cannot place blocks in water nor lava!");
				event.setCancelled(true);
				return;
			}
			// A block has been placed - push that the location should be air
			// (it was air before)
			blocks_changed.push(event.getBlockPlaced().getLocation());
			blocks_changed.push(Material.AIR);
			blocks_changed.push((byte) 0);

			if (checkForPillar(event.getPlayer(), event.getBlock(),
					pillarMaxHeight, 64))
				actOnPillar(event.getPlayer(), event.getBlock());
		}
		// When no game, no building
		else {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public synchronized void onBlockBreak(BlockBreakEvent event) {
		// If game is not resetting
		if (!resetting) {
			// Push the block being deleted for restorating later
			Block b = event.getBlock();

			blocks_changed.push(b.getLocation());
			blocks_changed.push(b.getType());

			@SuppressWarnings("deprecation")
			Byte data = b.getData();

			if (data != null) {
				blocks_changed.push(data);
			} else {
				blocks_changed.push((byte) 0);
			}
			// Does not appear to work
			if (b.getType() != Material.DIRT) {
				// Cancel and change to air
				event.setCancelled(true);
				b.setType(Material.AIR);
			}
		}
		// when no game, no building
		else {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void ignitefire(BlockIgniteEvent event) {
		// Try to stop fire
		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerCrouch(PlayerToggleSneakEvent event) {
		// Stop crouching as it prevents name tag showing
		event.setCancelled(true);
	}

	@EventHandler
	public void onWeatherChange(WeatherChangeEvent event) {
		// Try to stop weather
		getServer().getWorld(worldName).setWeatherDuration(1);
		event.setCancelled(true);
	}

	@EventHandler
	public void stopTakeOffHelmet(InventoryClickEvent event) {
		event.setCancelled(true);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("score")) {
			scoreSystem.GetTopFive(sender);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("myscore")) {
			scoreSystem.ReturnPersonalScore(sender);
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public boolean ifPillarBlock(Block b) {
		if ((b.getRelative(0, -1, 0) != null || b.getRelative(0, -1, 0)
				.getTypeId() != 0)
				&& b.getRelative(0, -1, 0).getType() == Material.DIRT) {
			if (b.getRelative(1, 0, 0) == null
					|| b.getRelative(1, 0, 0).getTypeId() == 0) {
				if (b.getRelative(-1, 0, 0) == null
						|| b.getRelative(-1, 0, 0).getTypeId() == 0) {
					if (b.getRelative(0, 0, 1) == null
							|| b.getRelative(0, 0, 1).getTypeId() == 0) {
						if (b.getRelative(0, 0, -1) == null
								|| b.getRelative(0, 0, -1).getTypeId() == 0) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean checkForPillar(Player p, Block b, int s, int a) {
		/*
		 * if (!(b.getY() < a)) { if (ifPillarBlock(b)) return true; for (int i
		 * = -s; i < 0; ++i) { if (!ifPillarBlock(b.getRelative(0, i, 0)))
		 * return false; } return true; } return false;
		 */
		if (!(b.getY() < a)) {
			switch (s) {
			case 0:
				if (ifPillarBlock(b))
					return true;
				break;
			case 1:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						return true;
				break;
			case 2:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							return true;
				break;
			case 3:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								return true;
				break;
			case 4:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									return true;
				break;
			case 5:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										return true;
				break;
			case 6:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										if (ifPillarBlock(b.getRelative(0, -6,
												0)))
											return true;
				break;
			case 7:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										if (ifPillarBlock(b.getRelative(0, -6,
												0)))
											if (ifPillarBlock(b.getRelative(0,
													-7, 0)))
												return true;
				break;
			case 8:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										if (ifPillarBlock(b.getRelative(0, -6,
												0)))
											if (ifPillarBlock(b.getRelative(0,
													-7, 0)))
												if (ifPillarBlock(b
														.getRelative(0, -8, 0)))
													return true;
				break;
			case 9:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										if (ifPillarBlock(b.getRelative(0, -6,
												0)))
											if (ifPillarBlock(b.getRelative(0,
													-7, 0)))
												if (ifPillarBlock(b
														.getRelative(0, -8, 0)))
													if (ifPillarBlock(b
															.getRelative(0, -9,
																	0)))
														return true;
				break;
			case 10:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										if (ifPillarBlock(b.getRelative(0, -6,
												0)))
											if (ifPillarBlock(b.getRelative(0,
													-7, 0)))
												if (ifPillarBlock(b
														.getRelative(0, -8, 0)))
													if (ifPillarBlock(b
															.getRelative(0, -9,
																	0)))
														if (ifPillarBlock(b
																.getRelative(0,
																		-10, 0)))
															return true;
				break;
			case 11:
				if (ifPillarBlock(b))
					if (ifPillarBlock(b.getRelative(0, -1, 0)))
						if (ifPillarBlock(b.getRelative(0, -2, 0)))
							if (ifPillarBlock(b.getRelative(0, -3, 0)))
								if (ifPillarBlock(b.getRelative(0, -4, 0)))
									if (ifPillarBlock(b.getRelative(0, -5, 0)))
										if (ifPillarBlock(b.getRelative(0, -6,
												0)))
											if (ifPillarBlock(b.getRelative(0,
													-7, 0)))
												if (ifPillarBlock(b
														.getRelative(0, -8, 0)))
													if (ifPillarBlock(b
															.getRelative(0, -9,
																	0)))
														if (ifPillarBlock(b
																.getRelative(0,
																		-10, 0)))
															if (ifPillarBlock(b
																	.getRelative(
																			0,
																			-11,
																			0)))
																return true;
				break;
			}
		}
		return false;
	}

	@SuppressWarnings({ "deprecation" })
	public boolean actOnPillar(Player p, Block b) {
		for (Integer blockY = pillarMaxHeight; blockY > -1; blockY--) {
			if (blockY != 0)
				b.getRelative(0, -(blockY), 0).setTypeId(0);
			else
				b.setTypeId(0);
		}
		p.sendMessage(ChatColor.RED + "Do not build pillars!");
		return false;
	}
}