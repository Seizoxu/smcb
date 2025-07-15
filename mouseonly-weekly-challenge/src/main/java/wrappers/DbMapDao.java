package wrappers;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import dataStructures.OsuMap;

public interface DbMapDao
{
	@SqlUpdate("""
			INSERT INTO maps (map_id, mapset_id, end_date, title, artist, mapper, difficulty_name, banner_link, star_rating, ar, od, hp, cs, length_seconds, bpm)
			VALUES (:map_id, :mapset_id, :end_date, :title, :artist, :mapper, :difficulty_name, :banner_link, :star_rating, :ar, :od, :hp, :cs, :length_seconds, :bpm)
			ON DUPLICATE KEY UPDATE
				mapset_id = VALUES(mapset_id),
				end_date = VALUES(end_date),
				title = VALUES(title),
				artist = VALUES(artist),
				mapper = VALUES(mapper),
				difficulty_name = VALUES(difficulty_name),
				banner_link = VALUES(banner_link),
				star_rating = VALUES(star_rating),
				ar = VALUES(ar),
				od = VALUES(od),
				hp = VALUES(hp),
				cs = VALUES(cs),
				length_seconds = VALUES(length_seconds),
				bpm = VALUES(bpm)
			""")
	void insertMap(
			@Bind("map_id") int mapId,
			@Bind("mapset_id") int mapsetId,
			@Bind("end_date") String endDate,
			@Bind("title") String title,
			@Bind("artist") String artist,
			@Bind("mapper") String mapper,
			@Bind("difficulty_name") String difficultyName,
			@Bind("banner_link") String bannerLink,
			@Bind("star_rating") double starRating,
			@Bind("ar") double ar,
			@Bind("od") double od,
			@Bind("hp") double hp,
			@Bind("cs") double cs,
			@Bind("length_seconds") int lengthSeconds,
			@Bind("bpm") int bpm);
	
	@SqlUpdate("DELETE FROM maps WHERE map_id = :map_id")
	void removeMap(@Bind("map_id") int mapId);
	
	@SqlQuery("SELECT * FROM maps WHERE map_id = :map_id")
	Optional<OsuMap> getMap(@Bind("map_id") int mapId);

	@SqlQuery("SELECT * FROM maps")
	List<OsuMap> getAllMaps();
	
	@SqlQuery("SELECT * FROM maps WHERE end_date = :sql_date")
	List<OsuMap> getMapsOfEndDate(@Bind("sql_date") String sqlDate);
	
	// Interval 6 day, because BETWEEN is inclusive on both ends; this is a 7 day window.
	@SqlQuery("""
			SELECT 1 FROM maps
			WHERE map_id = :map_id
				AND :score_date >= DATE_SUB(CAST(end_date AS DATETIME), INTERVAL 6 DAY)
				AND :score_date < DATE_ADD(CAST(end_date AS DATETIME), INTERVAL 1 DAY)

			""")
	Optional<Integer> isMapInSubmissionWindow(@Bind("map_id") int mapId, @Bind("score_date") Timestamp scoreDate);
}
