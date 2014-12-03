package stoneharry.zvh;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class GameEndCheck implements Runnable {
	@Override
	public void run() {
		try {
			while (!Main.ShuttingDown) {
				if (Main.gameRunning && !Main.resetting) {
					int result = Main.CheckForTimeUp();
					if (Main.humans.isEmpty()
							|| Bukkit.getServer().getOnlinePlayers().size() == 0) {
						if (result != 0) {
							Bukkit.broadcastMessage(ChatColor.RED
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