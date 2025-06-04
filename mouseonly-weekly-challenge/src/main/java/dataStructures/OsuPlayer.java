package dataStructures;

import java.beans.ConstructorProperties;

public class OsuPlayer
{
	private long userId;
	private String username;
	private String countryCode;
	private boolean isVerified;
	private long discordId;
	
	@ConstructorProperties({"user_id", "username", "country_code", "verified", "discord_id"})
	public OsuPlayer(long userId, String username, String countryCode, boolean isVerified, long discordId)
	{
		this.userId = userId;
		this.username = username;
		this.countryCode = countryCode;
		this.isVerified = isVerified;
		this.discordId = discordId;
	}

	public long getUserId() {return userId;}
	public void setUserId(long userId) {this.userId = userId;}

	public String getUsername(){return username;}
	public void setUsername(String username) {this.username = username;}

	public String getCountryCode() {return countryCode;}
	public void setCountryCode(String countryCode) {this.countryCode = countryCode;}
	
	public boolean isVerified() {return isVerified;}
	public void setVerified(boolean isVerified) {this.isVerified = isVerified;}

	public long getDiscordId() {return discordId;}
	public void setDiscordId(long discordId) {this.discordId = discordId;}
}
