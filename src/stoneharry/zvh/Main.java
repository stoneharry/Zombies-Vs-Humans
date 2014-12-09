package stoneharry.zvh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class Main extends JavaPlugin implements Listener {
	public static Main instance = null;
	public static Logger console = null;
	public static HashSet<String> humans;
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
	public static String worldName = "zombies";
	public static Location[] RoundHumanLocations;
	public static Location[] RoundZombieLocations;
	public static int pillarMaxHeight = 2;
	private static ScoreboardManager manager = null;
	public static Scoreboard board = null;
	public static Objective objective = null;
	private HashMap<String, ItemStack[]> inventories = new HashMap<String, ItemStack[]>();
	private HashMap<String, ItemStack[]> armour = new HashMap<String, ItemStack[]>();
	private String homeWorldName = null;
	private int[] homeWorldCoords = null;

	private ConsoleCommandSender commandConsole = Bukkit.getServer()
			.getConsoleSender();

	private boolean checkPlayer(Player p) {
		if (p != null && p.getWorld().getName().equals(worldName))
			return true;
		return false;
	}

	private void restoreInventory(Player p) {
		if (p == null)
			return;
		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		String name = p.getName();
		ItemStack[] items = inventories.get(name);
		ItemStack[] arm = armour.get(name);
		if (items != null) {
			for (ItemStack item : items) {
				if (item != null)
					p.getInventory().addItem(item);
			}
			inventories.remove(name);
		}
		if (arm != null) {
			p.getInventory().setArmorContents(arm);
			armour.remove(name);
		}
		humans.remove(name);
		p.setDisplayName(name);
		p.setPlayerListName(name);
		Scoreboard emptyBoard = manager.getNewScoreboard();
		p.setScoreboard(emptyBoard);
	}

	private void saveInventory(Player p, boolean teleport) {
		if (p == null)
			return;
		String name = p.getName();
		inventories.put(name, p.getInventory().getContents().clone());
		armour.put(name, p.getInventory().getArmorContents());
		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		p.setGameMode(GameMode.SURVIVAL);
		p.setScoreboard(board);
		if (p.isOnline()) {
			if (teleport)
				HandleRounds.handleTeleport(p, false);
			p.sendMessage(ChatColor.RED + prefix + " " + ChatColor.AQUA
					+ "You can view your personal score with: /myscore.");
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
				if (humans.contains(p.getName())) {
					humans.remove(p.getName());
				}
			}
		}
	}

	@EventHandler
	private void OnPlayerTeleport(PlayerTeleportEvent event) {
		/*
		 * if (event.getTo() != null && event.getTo().getWorld() != null) { if
		 * (event.getTo().getWorld().getName().equals(worldName))
		 * saveInventory(event.getPlayer(), false); } else if (event.getFrom()
		 * != null && event.getFrom().getWorld() != null) { if
		 * (event.getFrom().getWorld().getName().equals(worldName))
		 * restoreInventory(event.getPlayer()); }
		 */
	}

	public static List<Player> getPlayers() {
		Collection<? extends Player> plrs = Bukkit.getOnlinePlayers();
		List<Player> returnVal = new LinkedList<Player>();
		for (Player p : plrs) {
			if (p.getWorld().getName().equals(worldName))
				returnVal.add(p);
		}
		return returnVal;
	}

	private void LoadConfig() {
		// The following method will not overwrite an existing file.
		saveDefaultConfig();
		timeLimit = getConfig().getInt("timeLimit");
		prefix = getConfig().getString("ServerName");
		numRounds = getConfig().getInt("NumArenas");
		worldName = getConfig().getString("WorldName");
		homeWorldName = getConfig().getString("HomeName");
		homeWorldCoords = new int[] { getConfig().getInt("HomeX"),
				getConfig().getInt("HomeY"), getConfig().getInt("HomeZ") };

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
			for (Player p : getPlayers()) {
				p.teleport(new Location(Bukkit.getWorld(homeWorldName),
						homeWorldCoords[0], homeWorldCoords[1],
						homeWorldCoords[2]));
				restoreInventory(p);
			}
			resetLevelNow();
			blocks_changed.clear();
			humans.clear();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			ShuttingDown = true;
		}
		try {
			SaveScores scores = new SaveScores(objective);
			FileOutputStream fs = new FileOutputStream(new File("scores.cache"));
			ObjectOutputStream os = new ObjectOutputStream(fs);
			os.writeObject(scores);
			fs.close();
			os.close();
		} catch (Exception ex) {
			ex.printStackTrace();
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
		humans = new HashSet<String>();
		commandConsole = Bukkit.getServer().getConsoleSender();
		// Register events
		getServer().getPluginManager().registerEvents(this, this);
		// Load the config
		LoadConfig();
		// Get scores
		try {
			FileInputStream fs = new FileInputStream(new File("scores.cache"));
			ObjectInputStream os = new ObjectInputStream(fs);
			SaveScores scores = (SaveScores) os.readObject();
			fs.close();
			List<String> players = scores.getPlayers();
			List<Integer> points = scores.getScores();
			int size = players.size();
			for (int i = 0; i < size; ++i)
				objective.getScore(players.get(i)).setScore(points.get(i));
			os.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// Check for time up and lightning every 28 seconds
		new Thread(new TimeRemaining()).start();
		// This thread gets a accurate game ending
		new Thread(new GameEndCheck()).start();
		// Annoying message
		commandConsole.sendMessage(ChatColor.AQUA + "########################");
		commandConsole.sendMessage(ChatColor.AQUA + "[StonedZombie] "
				+ ChatColor.RED + " Enabled!");
		commandConsole.sendMessage(ChatColor.AQUA + "########################");
	}

	public static int CheckForTimeUp() {
		// Get time elapsed
		long result = System.currentTimeMillis() - lastTime;
		// See if the time elapsed is more than the time limit
		if (result > timeLimit) {
			for (String name : humans)
				ScoreSystem.incrementSurvivorScore(name);
			for (Player p : getPlayers())
				p.sendMessage(ChatColor.RED
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
		if (!checkPlayer(event.getPlayer()))
			return;
		try {
			HandleMovement.HandleEvent(event);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (!checkPlayer(event.getPlayer()))
			return;
		if (event.getPlayer() != null) {
			event.getPlayer().teleport(
					new Location(Bukkit.getWorld(homeWorldName),
							homeWorldCoords[0], homeWorldCoords[1],
							homeWorldCoords[2]));
			restoreInventory(event.getPlayer());
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
		while (!Main.blocks_changed.isEmpty()) {
			Byte b;
			Material mat;
			Location loc;
			if (Main.blocks_changed.size() < 3) {
				Main.console.info("ERROR: Stack size was less than 3.");
				Main.blocks_changed.clear();
				continue;
			}
			Object aa = Main.blocks_changed.pop();
			Object bb = Main.blocks_changed.pop();
			Object cc = Main.blocks_changed.pop();
			if (aa instanceof Byte) {
				b = (Byte) aa;
			} else {
				Main.console
						.info("ERROR: First pop is not a instance of a Byte.");
				continue;
			}
			if (bb instanceof Material) {
				mat = (Material) bb;
			} else {
				Main.console
						.info("ERROR: Second pop is not a instance of a Integer.");
				continue;
			}
			if (cc instanceof Location) {
				loc = (Location) cc;
			} else {
				Main.console
						.info("ERROR: Third pop is not a instance of a Location.");
				continue;
			}

			loc.getBlock().setTypeIdAndData(mat.getId(), b, true);
		}

		for (World world : Bukkit.getServer().getWorlds()) {
			if (world.getName().equals(worldName)) {
				for (Entity e : world.getEntities()) {
					if (e instanceof Item) {
						e.remove();
					}
				}
				break;
			}
		}

		// Make sure the needed variables change
		Main.gameRunning = false;
		Main.resetting = false;
		Main.mark_zombies = false;
		HandleRounds.incrementRound();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		// When a player joins, set them to zombie and update them if game is
		// running, teleport.
		if (!checkPlayer(event.getPlayer()))
			return;
		saveInventory(event.getPlayer(), true);
		event.setJoinMessage(null);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player p = event.getPlayer();
		if (p.isOnline()) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
					"That username is already online.");
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
		if (checkPlayer(event.getPlayer())) {
			event.setQuitMessage(null);
			event.getPlayer().teleport(
					new Location(Bukkit.getWorld(homeWorldName),
							homeWorldCoords[0], homeWorldCoords[1],
							homeWorldCoords[2]));
			restoreInventory(event.getPlayer());
		}
	}

	// Make players immune to damage
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		Entity e = event.getEntity();
		if (e instanceof Player) {
			if (checkPlayer((Player) e))
				event.setCancelled(true);
		}
	}

	// Prevent players getting hungry
	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		Entity e = event.getEntity();
		if (e instanceof Player) {
			if (checkPlayer((Player) e))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public synchronized void onBlockPlace(BlockPlaceEvent event) {
		if (!checkPlayer(event.getPlayer()))
			return;
		// If game is in progress
		if (!resetting) {
			Material replaced = event.getBlockReplacedState().getType();
			if (replaced == Material.WATER
					|| replaced == Material.STATIONARY_WATER
					|| replaced == Material.LAVA
					|| replaced == Material.LAVA_BUCKET) {
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
		if (!checkPlayer(event.getPlayer()))
			return;
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
		Block b = event.getBlock();
		if (b != null && b.getWorld().getName().equals(worldName)) {
			// Try to stop fire
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerCrouch(PlayerToggleSneakEvent event) {
		if (!checkPlayer(event.getPlayer()))
			return;
		// Stop crouching as it prevents name tag showing
		event.setCancelled(true);
	}

	@EventHandler
	public void onWeatherChange(WeatherChangeEvent event) {
		// Try to stop weather
		if (event.getWorld().getName().equals(worldName)) {
			getServer().getWorld(worldName).setWeatherDuration(1);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void stopTakeOffHelmet(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			if (checkPlayer((Player) event.getWhoClicked()))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onCommandPre(PlayerCommandPreprocessEvent event) {
		Player p = event.getPlayer();
		if (checkPlayer(p)) {
			String message = event.getMessage();
			if (message.equals("/myscore") || message.equals("/playgame")) {
				event.setCancelled(false);
			} else {
				event.setCancelled(true);
				p.sendMessage(ChatColor.RED
						+ "To leave this game mode use: /playgame");
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("myscore")) {
			ScoreSystem.ReturnPersonalScore(sender);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("playgame")) {
			Player p = (Player) sender;
			if (p.getName().length() == 16) {
				p.sendMessage(ChatColor.RED
						+ "Your name is 1 character too long to join the game!");
				return true;
			}
			if (checkPlayer(p)) {
				p.teleport(new Location(Bukkit.getWorld(homeWorldName),
						homeWorldCoords[0], homeWorldCoords[1],
						homeWorldCoords[2]));
				restoreInventory(p);
			} else {
				Location l = Main.RoundZombieLocations[0];
				l.setWorld(Bukkit.getWorld(Main.worldName));
				p.teleport(l);
				saveInventory(p, true);
			}
		} else if (cmd.getName().equalsIgnoreCase("listworlds")) {
			for (World w : Bukkit.getWorlds()) {
				sender.sendMessage(w.getName());
			}
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