package wrappers;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface DbUserDao
{
	@SqlUpdate("""
			INSERT INTO users (user_id, username, country_code, verified)
			VALUES (:user_id, :username, :country_code, :verified)
			ON DUPLICATE KEY UPDATE
				username = VALUES(username),
				country_code = VALUES(country_code)
			""")
	void insertUser(@Bind("user_id") long userId, @Bind("username") String username, @Bind("country_code") String countryCode, @Bind("verified") boolean isVerified);
	
	@SqlQuery("SELECT user_id, username FROM users WHERE verified = 0;")
	void getUnverifiedUsers();
	
	@SqlUpdate("UPDATE users SET verified = TRUE WHERE user_id = :user_id")
	void verifyUser(@Bind("user_id") long userId);
	
	@SqlUpdate("UPDATE users SET verified = FALSE WHERE user_id = :user_id")
	void unverifyUser(@Bind("user_id") long userId);
}
