package dataStructures;

import java.beans.ConstructorProperties;

public class OsuPlayer
{
	private int userId;
	private String username;
	private String countryCode;
	private boolean isVerified;
	
	@ConstructorProperties({"user_id", "username", "country_code", "verified"})
	public OsuPlayer(int userId, String username, String countryCode, boolean isVerified)
	{
		this.userId = userId;
		this.username = username;
		this.countryCode = countryCode;
		this.isVerified = isVerified;
	}

	public int getUserId() {return userId;}
	public void setUserId(int userId) {this.userId = userId;}

	public String getName(){return username;}
	public void setName(String username) {this.username = username;}

	public String getCountryCode() {return countryCode;}
	public void setCountryCode(String countryCode) {this.countryCode = countryCode;}
	
	public boolean isVerified() {return isVerified;}
	public void setVerified(boolean isVerified) {this.isVerified = isVerified;}
}
