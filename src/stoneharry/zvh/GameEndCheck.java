package stoneharry.zvh;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

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
						ScoreboardManager manager = Bukkit
								.getScoreboardManager();
						Scoreboard board = manager.getNewScoreboard();
						Objective objective = board.registerNewObjective(
								"board", "dummy");
						objective.setDisplaySlot(DisplaySlot.SIDEBAR);
						objective
								.setDisplayName(ChatColor.AQUA + "High Scores");
						SortedSet<SortedBoard> list = new TreeSet<SortedBoard>();
						for (String str : Main.board.getEntries())
							list.add(new SortedBoard(str, Main.objective
									.getScore(str).getScore()));
						int size = list.size() > 14 ? 15 : list.size();
						Iterator<SortedBoard> it = list.iterator();
						int i = 0;
						while (it.hasNext() && i < size) {
							SortedBoard b = it.next();
							objective.getScore(b.player).setScore(b.score);
							++i;
						}
						for (Player p : players)
							p.setScoreboard(board);
					}
				}
				Thread.sleep(1000);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected class SortedBoard implements Comparable<SortedBoard> {
		public String player;
		public int score;

		public SortedBoard(String plr, int c) {
			player = plr;
			score = c;
		}

		@Override
		public int compareTo(SortedBoard o) {
			if (score > o.score)
				return -1;
			else if (score < o.score)
				return 1;
			return 0;
		}

	}
}