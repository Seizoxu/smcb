package dataStructures;

import java.beans.ConstructorProperties;
import java.time.Instant;

public class OsuScore
{
	private long scoreId;
	private long userId;
	private int mapId;
	private int score;
	private String[] mods;
	private Instant timestamp;

	@ConstructorProperties({"score_id", "user_id", "map_id", "mods", "timestamp"})
	public OsuScore(long scoreId, long userId, int mapId, int score, String[] mods, Instant timestamp)
	{
		this.scoreId = scoreId;
		this.userId = userId;
		this.mapId = mapId;
		this.score = score;
		this.mods = mods;
		this.timestamp = timestamp;
	}

	public long getScoreId() {return scoreId;}
	public void setScoreId(long scoreId) {this.scoreId = scoreId;}

	public long getUserId() {return userId;}
	public void setUserId(long userId) {this.userId = userId;}

	public int getMapId() {return mapId;}
	public void setMapId(int mapId) {this.mapId= mapId;}

	public int getScore() {return score;}
	public void setScore(int score) {this.score = score;}

	public String[] getMods() {return mods;}
	public void setMods(String[] mods) {this.mods = mods;}

	public Instant getTimestamp() {return timestamp;}
	public void setTimestamp(Instant timestamp) {this.timestamp = timestamp;}
}
