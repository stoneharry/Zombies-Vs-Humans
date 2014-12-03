package stoneharry.zvh;

import java.io.Serializable;
import java.util.ArrayList;

import org.bukkit.scoreboard.Objective;

public class SaveScores implements Serializable {
	private static final long serialVersionUID = 1088346158553072444L;

	private String[] players;
	private Integer[] scores;

	public SaveScores(Objective board) {
		ArrayList<String> players = new ArrayList<String>();
		ArrayList<Integer> scores = new ArrayList<Integer>();
		for (String plr : board.getScoreboard().getEntries()) {
			players.add(plr);
			scores.add(board.getScore(plr).getScore());
		}
		this.players = players.toArray(new String[players.size()]);
		this.scores = scores.toArray(new Integer[scores.size()]);
	}

	public String[] getPlayers() {
		return players;
	}

	public Integer[] getScores() {
		return scores;
	}
}
