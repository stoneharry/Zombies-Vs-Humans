package stoneharry.zvh;

import org.bukkit.entity.Player;

public class handleRounds {
	public static void handleTeleport(Player plr, Boolean human) {
		if (human) {
			plr.teleport(main.RoundHumanLocations[main.round - 1]);
		} else {
			plr.teleport(main.RoundZombieLocations[main.round - 1]);
		}
	}

	public static void incrementRound() {
		if (main.round == main.numRounds) {
			main.round = 1;
		} else {
			main.round = main.round + 1;
		}
	}
}