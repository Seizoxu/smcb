package wrappers;

import java.time.Instant;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import dataStructures.OsuScoreWithUser;

public interface DbScoreDao
{
	@SqlUpdate("""
			INSERT INTO scores (score_id, user_id, map_id, score, mods, timestamp)
			VALUES (:score_id, :user_id, :map_id, :score, :mods, :timestamp)
			ON DUPLICATE KEY UPDATE
				score = VALUES(score),
				mods = VALUES(mods),
				timestamp = VALUES(timestamp)
			""")
	void insertOrUpdateScore(
			@Bind("score_id") long scoreId,
			@Bind("user_id") long userId,
			@Bind("map_id") int mapId,
			@Bind("score") int score,
			@Bind("mods") String mods,
			@Bind("timestamp") Instant timestamp);
	
	@SqlUpdate("DELETE FROM score WHERE user_id = :user_id AND map_id = :map_id")
	void deleteScoreByUserAndMap(@Bind("user_id") long userId, @Bind("map_id") int mapId);
	
	@SqlQuery("""
			SELECT s.*, u.username FROM scores s
			JOIN users on u on s.user_id = u.user_id
			ORDER BY timestamp DESC
			""")
	List<OsuScoreWithUser> getAllScoresSortedByTime();
	
	@SqlQuery("""
			SELECT s.*, u.username FROM scores s
			JOIN users u ON s.user_id = u.user_id
			WHERE map_id = :map_id
			ORDER BY score DESC
			""")
	List<OsuScoreWithUser> getScoresForMapSorted(@Bind("map_id") int mapId);
}
