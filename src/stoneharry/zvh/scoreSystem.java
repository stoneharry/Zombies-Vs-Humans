package stoneharry.zvh;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Score;

public class scoreSystem {
	private static Connection connection;
	private static Connection authentication;
	private static PreparedStatement preparedStatement;
	public static Boolean enoughPlayers = false;

	public static boolean EstablishConnection() {
		try {
			if (!main.useScoreSystem) {
				return true;
			}
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connection = DriverManager.getConnection("jdbc:mysql://"
					+ main.SQLAddress + ":" + main.SQLPort + "/"
					+ main.ZombiesDB, main.SQLUsername, main.SQLPassword);
			if (connection.isClosed()) {
				return false;
			}
			authentication = DriverManager.getConnection("jdbc:mysql://"
					+ main.SQLAddress + ":" + main.SQLPort + "/"
					+ main.AuthMeDB, main.SQLUsername, main.SQLPassword);
			if (authentication.isClosed()) {
				return false;
			}
		} catch (Exception ex) {
			// ex.printStackTrace();
			return false;
		}
		return true;
	}

	public static void DropConnection() {
		try {
			if (!main.useScoreSystem) {
				return;
			}
			preparedStatement.close();
			connection.close();
			authentication.close();
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
	}

	public static void zombiePointIncrement(String plr) {
		try {
			if (enoughPlayers && main.useScoreSystem) {
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `score` = score+1 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `infected` = infected+1 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `points` = points+1 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
			}
			Score score = main.objective.getScore(plr);
			score.setScore(score.getScore() + 1);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void incrementTimerScore(String plr) {
		try {
			if (enoughPlayers && main.useScoreSystem) {
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `score` = score+3 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `points` = points+3 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
			}
			Score score = main.objective.getScore(plr);
			score.setScore(score.getScore() + 3);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void incrementSurvivorScore(String plr) {
		try {
			if (enoughPlayers && main.useScoreSystem) {
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `survived` = survived+1 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `score` = score+5 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
				preparedStatement = connection
						.prepareStatement("UPDATE `score_system` SET `points` = points+5 WHERE `name` = '"
								+ plr + "'");
				preparedStatement.executeUpdate();
			}
			Score score = main.objective.getScore(plr);
			score.setScore(score.getScore() + 5);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void addPlayerToScoreIfNotExists(String plr) {
		try {
			if (main.useScoreSystem) {
				preparedStatement = connection
						.prepareStatement("SELECT * FROM `score_system` WHERE `name` = '"
								+ plr + "'");
				ResultSet rs = preparedStatement.executeQuery();
				if (!rs.first()) {
					PreparedStatement statement = connection
							.prepareStatement("INSERT INTO `score_system` VALUES ('"
									+ plr + "', '0', '0', '0', '0')");
					statement.executeUpdate();
					statement.close();
				}
				rs.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void ReturnPersonalScore(CommandSender sender) {
		try {
			if (main.useScoreSystem) {
				preparedStatement = connection
						.prepareStatement("SELECT `name`,`score`,`points` FROM `score_system` WHERE `name` = '"
								+ sender.getName() + "'");
				ResultSet rs = preparedStatement.executeQuery();
				sender.sendMessage(ChatColor.GREEN
						+ "-- Personal Score ----------------");
				if (rs.first()) {
					String name = rs.getString(1);
					String score = rs.getString(2);
					sender.sendMessage(ChatColor.GREEN
							+ "Name | Score | Points");
					sender.sendMessage(ChatColor.GREEN + name + " | " + score
							+ " | " + rs.getString(3));
				}
				rs.close();
			} else {
				sender.sendMessage(ChatColor.GREEN
						+ "The score system is not enabled.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void GetTopFive(CommandSender sender) {
		try {
			if (main.useScoreSystem) {
				preparedStatement = connection
						.prepareStatement("SELECT `name`,`score` FROM `score_system` ORDER BY `score` DESC LIMIT 5");
				ResultSet rs = preparedStatement.executeQuery();
				int place = 0;
				sender.sendMessage(ChatColor.GREEN
						+ "-- Top Scores --------------------");
				if (rs.first()) {
					place = place + 1;
					String name = rs.getString(1);
					String score = rs.getString(2);
					sender.sendMessage(ChatColor.AQUA + String.valueOf(place)
							+ ": " + ChatColor.GREEN + name + " | " + score);
					while (rs.next()) {
						place = place + 1;
						name = rs.getString(1);
						score = rs.getString(2);
						sender.sendMessage(ChatColor.AQUA
								+ String.valueOf(place) + ": "
								+ ChatColor.GREEN + name + " | " + score);
					}
				}
				rs.close();
			} else {
				sender.sendMessage(ChatColor.GREEN
						+ "The score system is not enabled.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void Register(CommandSender sender, String password) {
		if (!main.useScoreSystem || !main.useAuthMe) {
			sender.sendMessage(ChatColor.GREEN + "The system is not enabled.");
			return;
		}

		password = getSHA1(password);

		if (password.length() == 0) {
			sender.sendMessage(ChatColor.GREEN
					+ "An unknown error happened when encrypting your password. Claim failed.");
			return;
		}

		try {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.GREEN + "Console cannot register.");
				return;
			}

			if (alreadyRegistered(sender.getName())) {
				sender.sendMessage(ChatColor.GREEN
						+ "That username is already registered.");
				return;
			}

			Player p = (Player) sender;

			String query = "INSERT INTO `authme` (`username`, `password`, `lastlogin`, `ip`) "
					+ "VALUES ('"
					+ p.getName()
					+ "', '"
					+ password
					+ "', "
					+ "'"
					+ System.currentTimeMillis()
					+ "', "
					+ "'"
					+ p.getAddress().getAddress().getHostAddress() + "')";

			preparedStatement = authentication.prepareStatement(query);
			preparedStatement.executeUpdate();

			sender.sendMessage(ChatColor.GREEN
					+ "You have been registered successfully! This same account will work on the other Server servers.");
		} catch (Exception ex) {
			sender.sendMessage(ChatColor.GREEN
					+ "An unknown error happend when attempting to register you. Register failed.");
		}
	}

	public static Boolean alreadyRegistered(String name) {
		try {
			preparedStatement = authentication
					.prepareStatement("SELECT `id` FROM `zombies` WHERE `name` = '"
							+ name + "'");
			ResultSet rs = preparedStatement.executeQuery();
			if (!rs.first()) {
				rs.close();
				return false;
			}
			rs.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return true;
		}
	}

	public static void Claim(CommandSender sender, String password, String ID) {
		if (!main.useScoreSystem || !main.useAuthMe) {
			sender.sendMessage(ChatColor.GREEN + "The system is not enabled.");
			return;
		}

		password = getSHA1(password);

		if (password.length() == 0) {
			sender.sendMessage(ChatColor.GREEN
					+ "An unknown error happened when encrypting your password. Claim failed.");
			return;
		}

		String name = sender.getName();

		if (!authenticateSuccess(name, password)) {
			sender.sendMessage(ChatColor.GREEN
					+ "That username and password combination was not correct. Are you registered? Use /register Password. If you already have an account on the survival server, that is the password you must use here.");
			return;
		}

		if (!confirmID(name, ID)) {
			sender.sendMessage(ChatColor.GREEN
					+ "You do not have any of that to claim.");
			return;
		}

		try {
			int num = Integer.parseInt(ID);
			if (num == 1) {
				sender.sendMessage(ChatColor.GREEN
						+ "Added 10 more dirt to you!");
				Player p = (Player) sender;
				p.getInventory().addItem(new ItemStack(Material.DIRT, 10));
				p.updateInventory();
			} else if (num == 2) {
				sender.sendMessage(ChatColor.GREEN
						+ "Added 100 more dirt to you!");
				Player p = (Player) sender;
				p.getInventory().addItem(new ItemStack(Material.DIRT, 100));
				p.updateInventory();
			} else {
				sender.sendMessage(ChatColor.GREEN
						+ "You have this reward but it is not implemented. What?");
			}
		} catch (Exception ex) {
			sender.sendMessage(ChatColor.GREEN
					+ "Something went wrong trying to claim your purchase.");
			ex.printStackTrace();
		}
	}

	private static Boolean confirmID(String name, String ID) {
		try {
			preparedStatement = authentication
					.prepareStatement("SELECT `entry`,`amount` FROM `zombies` WHERE `name` = '"
							+ name + "' AND `id` = '" + ID + "'");
			ResultSet rs = preparedStatement.executeQuery();
			if (!rs.first()) {
				rs.close();
				return false;
			}

			int amount = Integer.parseInt(rs.getString(2));
			if (amount > 1) {
				preparedStatement = authentication
						.prepareStatement("UPDATE `zombies` SET `amount` = amount-1 WHERE `name` = '"
								+ name + "' AND `id` = '" + ID + "'");
				preparedStatement.executeUpdate();
			} else {
				preparedStatement = authentication
						.prepareStatement("DELETE FROM `zombies` WHERE `name` = '"
								+ name + "' AND `id` = '" + ID + "'");
				preparedStatement.executeUpdate();
			}

			rs.close();

			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	private static String getSHA1(String message) {
		String pass = "";
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			sha1.reset();
			sha1.update(message.getBytes());
			byte[] digest = sha1.digest();

			pass = String.format("%0" + (digest.length << 1) + "x",
					new Object[] { new BigInteger(1, digest) });
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		return pass;
	}

	private static Boolean authenticateSuccess(String name, String password) {
		try {
			preparedStatement = authentication
					.prepareStatement("SELECT `id` FROM `authme` WHERE `username` = '"
							+ name + "' AND `password` = '" + password + "'");
			ResultSet rs = preparedStatement.executeQuery();
			if (!rs.first()) {
				rs.close();
				return false;
			}
			rs.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
}