package stoneharry.zvh;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class GameEndCheck implements Runnable {
	@Override
	public void run() {
		try {
			while (!Main.ShuttingDown) {
				if (Main.gameRunning && !Main.resetting) {
					int result = Main.CheckForTimeUp();
					List<Player> players = Main.getPlayers();
					if (players.size() == 0 || Main.humans.isEmpty()) {
						if (result != 0) {
							for (Player p : players)
								p.sendMessage(ChatColor.RED
										+ Main.prefix
										+ " "
										+ ChatColor.AQUA
										+ "All humans have been caught! The game will end in 10 seconds...");
						}
						Main.prepareReset();
					}
				}
				Thread.sleep(1000);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}