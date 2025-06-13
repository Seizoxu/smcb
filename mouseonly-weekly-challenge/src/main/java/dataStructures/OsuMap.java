package dataStructures;

import java.beans.ConstructorProperties;

public class OsuMap
{
	private int mapId;
	private int mapsetId;
	private String endDate;
	private String title;
	private String artist;
	private String mapper;
	private String difficultyName;
	private String bannerLink;
	private double starRating;
	private double ar;
	private double od;
	private double hp;
	private double cs;
	private int lengthSeconds;
	private int bpm;

	@ConstructorProperties({"map_id", "mapset_id", "end_date", "title", "artist", "mapper", "difficulty_name", "banner_link", "star_rating", "ar", "od", "hp", "cs", "length_seconds", "bpm"})
	public OsuMap(int mapId, int mapsetId, String endDate, String title, String artist, String mapper, String difficultyName,
			String bannerLink, double starRating, double ar, double od, double hp, double cs, int lengthSeconds, int bpm)
	{
		this.mapId = mapId;
		this.mapsetId = mapsetId;
		this.endDate = endDate;
		this.title = title;
		this.artist = artist;
		this.mapper = mapper;
		this.difficultyName = difficultyName;
		this.bannerLink = bannerLink;
		this.starRating = starRating;
		this.ar = ar;
		this.od = od;
		this.hp = hp;
		this.cs = cs;
		this.lengthSeconds = lengthSeconds;
		this.bpm = bpm;
	}

	public int getMapId() {return mapId;}
	public void setMapId(int mapId) {this.mapId = mapId;}

	public int getMapsetId() {return mapsetId;}
	public void setMapsetId(int mapsetId) {this.mapsetId = mapsetId;}

	public String getEndDate() {return endDate;}
	public void setEndDate(String endDate) {this.endDate = endDate;}

	public String getTitle() {return title;}
	public void setTitle(String title) {this.title = title;}

	public String getArtist() {return artist;}
	public void setArtist(String artist) {this.artist = artist;}

	public String getMapper() {return mapper;}
	public void setMapper(String mapper) {this.mapper = mapper;}

	public String getDifficultyName() {return difficultyName;}
	public void setDifficultyName(String difficultyName) {this.difficultyName = difficultyName;}

	public String getBannerLink() {return bannerLink;}
	public void setBannerLink(String bannerLink) {this.bannerLink = bannerLink;}

	public double getStarRating() {return starRating;}
	public void setStarRating(double starRating) {this.starRating = starRating;}

	public double getAr() {return ar;}
	public void setAr(double ar){this.ar = ar;}

	public double getOd() {return od;}
	public void setOd(double od) {this.od = od;}

	public double getHp() {return hp;}
	public void setHp(double hp) {this.hp = hp;}

	public double getCs() {return cs;}
	public void setCs(double cs) {this.cs = cs;}

	public int getLengthSeconds() {return lengthSeconds;}
	public void setLengthSeconds(int lengthSeconds) {this.lengthSeconds = lengthSeconds;}

	public int getBpm() {return bpm;}
	public void setBpm(int bpm) {this.bpm = bpm;}
}
