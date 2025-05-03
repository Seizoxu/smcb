package dataStructures;

import java.beans.ConstructorProperties;
import java.time.Instant;

public class OsuScoreWithUser
{
	private long scoreId;
	private int userId;
	private int mapId;
	private int score;
	private String username;
	private String[] mods;
	private Instant timestamp;
	
	@ConstructorProperties({"score_id", "user_id", "map_id", "score", "username", "mods", "timestamp"})
	public OsuScoreWithUser(long scoreId, int userId, int mapId, int score, String username, String[] mods, Instant timestamp)
	{
		this.scoreId = scoreId;
		this.userId = userId;
		this.mapId = mapId;
		this.score = score;
		this.username = username;
		this.mods = mods;
		this.timestamp = timestamp;
	}

	public long getScoreId() {return scoreId;}
	public void setScoreId(long scoreId) {this.scoreId = scoreId;}

	public int getUserId() {return userId;}
	public void setUserId(int userId) {this.userId = userId;}

	public int getMapId() {return mapId;}
	public void setMapId(int mapId) {this.mapId= mapId;}

	public int getScore() {return score;}
	public void setScore(int score) {this.score = score;}

	public String getUsername() {return username;}
	public void setUsername(String username) {this.username = username;}

	public String[] getMods() {return mods;}
	public void setMods(String[] mods) {this.mods = mods;}

	public Instant getTimestamp() {return timestamp;}
	public void setTimestamp(Instant timestamp) {this.timestamp = timestamp;}
}
