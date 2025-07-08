package wrappers;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import dataStructures.OsuPlayer;

public interface DbUserDao
{
	@SqlUpdate("""
			INSERT INTO users (user_id, username, country_code, verified, discord_id)
			VALUES (:user_id, :username, :country_code, :verified, :discord_id)
			ON DUPLICATE KEY UPDATE
				username = VALUES(username),
				country_code = VALUES(country_code),
				discord_id = VALUES(discord_id)
			""")
	void insertUser(
			@Bind("user_id") long userId,
			@Bind("username") String username,
			@Bind("country_code") String countryCode,
			@Bind("verified") boolean isVerified,
			@Bind("discord_id") long discordId);
	
	@SqlQuery("SELECT EXISTS (SELECT 1 FROM users WHERE user_id = :user_id)")
	boolean userExists(@Bind("user_id") long userId);
	
	@SqlQuery("SELECT * FROM users WHERE verified = 0;")
	List<OsuPlayer> getUnverifiedUsers();
	
	@SqlUpdate("UPDATE users SET verified = TRUE WHERE user_id = :user_id")
	void verifyUser(@Bind("user_id") long userId);
	
	@SqlUpdate("UPDATE users SET verified = FALSE WHERE user_id = :user_id")
	void unverifyUser(@Bind("user_id") long userId);
	
	@SqlUpdate("UPDATE users SET discord_id = :discord_id WHERE user_id = :user_id")
	void updateDiscordId(@Bind("user_id") long userId, @Bind("discord_id") long discordId);
	
	@SqlQuery("SELECT * FROM users WHERE user_id = :user_id")
	OsuPlayer getUsername(@Bind("user_id") long userId);

	@SqlQuery("SELECT * FROM users WHERE discord_id = :discord_id")
	OsuPlayer getUserFromDiscordId(@Bind("discord_id") long discordId);
}
