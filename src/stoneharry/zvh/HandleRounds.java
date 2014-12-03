package stoneharry.zvh;

import org.bukkit.entity.Player;

public class HandleRounds {
	public static void handleTeleport(Player plr, Boolean human) {
		if (human) {
			plr.teleport(Main.RoundHumanLocations[Main.round - 1]);
		} else {
			plr.teleport(Main.RoundZombieLocations[Main.round - 1]);
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