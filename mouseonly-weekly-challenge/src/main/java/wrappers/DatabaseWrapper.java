package wrappers;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import dataStructures.OsuMap;
import dataStructures.OsuPlayer;

public class DatabaseWrapper
{
	private final Jdbi jdbi;
	private final DbUserDao userDao;
	private final DbScoreDao scoreDao;
	private final DbMapDao mapDao;
	
	public DatabaseWrapper(String jdbcUrl, String user, String password)
	{
		jdbi = Jdbi.create(jdbcUrl, user, password);
		jdbi.installPlugin(new SqlObjectPlugin());
		
		this.userDao = jdbi.onDemand(DbUserDao.class);
		this.scoreDao = jdbi.onDemand(DbScoreDao.class);
		this.mapDao = jdbi.onDemand(DbMapDao.class);
		
		jdbi.registerRowMapper(ConstructorMapper.factory(OsuMap.class));
		jdbi.registerRowMapper(ConstructorMapper.factory(OsuPlayer.class));
	}
	
	public DbUserDao getUserDao()
	{
		return userDao;
	}

	public DbScoreDao getScoreDao()
	{
		return scoreDao;
	}

	public DbMapDao getMapDao()
	{
		return mapDao;
	}
	
	public void createTables()
	{
		jdbi.useHandle(handle -> {
			handle.execute("""
					CREATE TABLE IF NOT EXISTS users (
						user_id BIGINT PRIMARY KEY,
						name VARCHAR(255) NOT NULL,
						country_code CHAR(2),
						verified BOOLEAN NOT NULL DEFAULT FALSE,
						discord_id BIGINT NOT NULL DEFAULT -1
					)
					""");
			handle.execute("""
					CREATE TABLE IF NOT EXISTS maps (
						map_id INT PRIMARY KEY,
						mapset_id INT NOT NULL,
						end_date DATE NOT NULL,
						title VARCHAR(512),
						artist VARCHAR(255),
						mapper VARCHAR(255),
						difficulty_name VARCHAR(255),
						banner_link TEXT,
						star_rating FLOAT,
						ar FLOAT,
						od FLOAT,
						hp FLOAT,
						cs FLOAT,
						length_seconds INT,
						bpm INT
					)
					""");
			handle.execute("""
					CREATE TABLE IF NOT EXISTS scores (
						score_id BIGINT,
						user_id BIGINT,
						map_id INT,
						score INT,
						mods VARCHAR(255),
						score_time DATETIME,
						FOREIGN KEY (user_id) REFERENCES users(user_id),
						FOREIGN KEY (map_id) REFERENCES maps(map_id),
						PRIMARY KEY (user_id, map_id)
					)
					""");
		});
	}
}
