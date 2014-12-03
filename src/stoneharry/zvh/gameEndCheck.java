package stoneharry.zvh;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class gameEndCheck implements Runnable {
	@Override
	public void run() {
		try {
			while (!main.ShuttingDown) {
				if (main.gameRunning && !main.resetting) {
					int result = main.CheckForTimeUp();
					if (main.humans.isEmpty()
							|| Bukkit.getServer().getOnlinePlayers().size() == 0) {
						if (result != 0) {
							Bukkit.broadcastMessage(ChatColor.RED
									+ main.prefix
									+ " "
									+ ChatColor.AQUA
									+ "All humans have been caught! The game will end in 10 seconds...");
						}
						main.prepareReset();
					}
				}
				Thread.sleep(1000);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}