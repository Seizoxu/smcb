package wrappers;

import java.sql.Timestamp;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import dataStructures.OsuScoreWithUser;

public interface DbScoreDao
{
	@SqlCall("CALL insert_or_update_score_if_higher(:score_id, :user_id, :map_id, :score, :mods, :score_time)")
	void callInsertOrUpdateScoreIfHigher(
			@Bind("score_id") long scoreId,
			@Bind("user_id") long userId,
			@Bind("map_id") int mapId,
			@Bind("score") int score,
			@Bind("mods") String mods,
			@Bind("score_time") Timestamp scoreTime);
	
	@SqlUpdate("DELETE FROM scores WHERE user_id = :user_id AND map_id = :map_id")
	void deleteScoreByUserAndMap(@Bind("user_id") long userId, @Bind("map_id") int mapId);
	
	@SqlUpdate("DELETE FROM scores WHERE score_id = :score_id")
	void deleteScoreByScoreId(@Bind("score_id") long scoreId);
	
	@SqlQuery("""
			SELECT s.*, u.username FROM scores s
			JOIN users on u on s.user_id = u.user_id
			ORDER BY `score_time` DESC
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
