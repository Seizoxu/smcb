package wrappers;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface DbMapDao
{
	@SqlUpdate("""
			INSERT INTO maps (map_id, mapset_id, title, artist, mapper, star_rating, ar, od, hp, cs, length_seconds, bpm, banner_link)
			VALUES (:map_id, :mapset_id, :title, :artist, :mapper, :star_rating, :ar, :od, :hp, :cs, :length_seconds, :bpm, :banner_link)
			ON DUPLICATE KEY UPDATE
				mapset_id = VALUES(mapset_id),
				title = VALUES(title),
				artist = VALUES(artist),
				mapper = VALUES(mapper),
				star_rating = VALUES(star_rating),
				ar = VALUES(ar),
				od = VALUES(od),
				hp = VALUES(hp),
				cs = VALUES(cs),
				length_seconds = VALUES(length_seconds),
				bpm = VALUES(bpm),
				banner_link = VALUES(banner_link)
			""")
	void insertMap(
			@Bind("map_id") int mapId,
			@Bind("mapset_id") int mapsetId,
			@Bind("title") String title,
			@Bind("artist") String artist,
			@Bind("mapper") String mapper,
			@Bind("star_rating") double starRating,
			@Bind("ar") double ar,
			@Bind("od") double od,
			@Bind("hp") double hp,
			@Bind("cs") double cs,
			@Bind("length_seconds") int lengthSeconds,
			@Bind("bpm") int bpm,
			@Bind("banner_link") String bannerLink);
}
