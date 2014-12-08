package stoneharry.zvh;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HandleRounds {
	public static void handleTeleport(Player plr, boolean human) {
		if (plr.getWorld() != null
				&& plr.getWorld().getName().equals(Main.worldName)) {
			if (human) {
				Location l = Main.RoundHumanLocations[Main.round - 1];
				l.setWorld(Bukkit.getWorld(Main.worldName));
				plr.teleport(l);
			} else {
				Location l = Main.RoundZombieLocations[Main.round - 1];
				l.setWorld(Bukkit.getWorld(Main.worldName));
				plr.teleport(l);
			}

		}
	}

	public static void incrementRound() {
		if (Main.round == Main.numRounds) {
			Main.round = 1;
		} else {
			Main.round = Main.round + 1;
		}
	}
}