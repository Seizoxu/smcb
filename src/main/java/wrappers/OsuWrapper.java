package wrappers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;


public class OsuWrapper
{
	private String clientId;
	private String clientSecret;
	private String legacyToken;
	private String token;
	private long tokenExpiryEpoch;
	
	public OsuWrapper(String clientId, String clientSecret, String legacyToken)
	{
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.legacyToken = legacyToken;
		
		try {updateOsuAuthentication();}
		catch (IOException | InterruptedException e) {e.printStackTrace();}
	}
	
	
	/**
	 * Refreshes the osu! API token.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void updateOsuAuthentication() throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://osu.ppy.sh/oauth/token"))
				.headers("Accept", "application/json", "Content-Type", "application/json")
				.POST
				(
						HttpRequest.BodyPublishers.ofString
						(
								String.format
								(
										"{\"client_id\": %s, "
										+ "\"client_secret\": \"%s\", "
										+ "\"grant_type\": \"%s\", "
										+ "\"scope\": \"%s\"}",
										clientId, clientSecret, "client_credentials", "public"
								)
						)
				)
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		
		JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
		token = json.get("access_token").getAsString();
		tokenExpiryEpoch = System.currentTimeMillis()/1000 + Long.parseLong(json.get("expires_in").toString());
	}

	
	/**
	 * osuAccessToken get method.
	 * @return An always-valid access token, at least for the
	 * next 30 minutes.
	 */
	private String getAccessToken()
	{
		try
		{
			// 1800 seconds = 30 minutes
			if (System.currentTimeMillis()/1000 + 1800 > tokenExpiryEpoch) {return token;}
			
			updateOsuAuthentication();
			return token;
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Generic method for requesting data from osu API v2.
	 * @param requestStr
	 * @return String formatted in JSON.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String requestData(String requestStr) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://osu.ppy.sh/api/v2/" + requestStr))
				.headers(
						"Authorization", "Bearer " + getAccessToken(),
						"x-api-version", "20240529")
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	
	
	/**
	 * Generic method for requesting data from osu API v1.
	 * @param requestStr
	 * @return String formatted in JSON.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String requestDataLegacy(String requestStr) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(String.format("https://osu.ppy.sh/api/%s&k=%s", requestStr, legacyToken)))
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	

	/* --------------------------------------------------------------------------------------------------
	 * --------------------------------------------------------------------------------------------------
	 * -------------------------------------------------------------------------------------------------- */
	

	/**
	 * STD PP rankings.
	 * @param country
	 * @return Optional<JsonObject> - List of n=50 osu! API v2 "Rankings" Structures.
	 */
	public Optional<JsonObject> getUsersByRanking(String country, int cursor)
	{
		try
		{
			String jsonStr = requestData(
					"rankings/osu/performance?mode=osu"
					+ "&country=" + country
					+ "&page=" + cursor);
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonObject());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return Optional.empty();}
	}
	

	/**
	 * Gets all ranked/loved STD beatmaps since a certain date.
	 * @param sinceDate: A MySQL-formatted DATE String.
	 * @return Optional<JsonArray> - List of n<=500 osu! API v1 "Beatmap" Structures.
	 */
	public Optional<JsonArray> getBeatmapsSinceDate(String sinceDate)
	{
		try
		{
			String jsonStr = requestDataLegacy(String.format(
					"get_beatmaps?since=%s&m=0",
					sinceDate
					));
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonArray());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return Optional.empty();}
	}
	
	
	/**
	 * Returns a list of beatmaps given an int-array of IDs (maximum 50).
	 * @param ids
	 * @return Optional<JsonArray> - List of osu! API v2 "BeatmapExtended" Structure.
	 */
	public Optional<JsonArray> getBeatmapsByIds(int[] ids)
	{
		try
		{
			String idQueries = "?";
			for (int i : ids)
			{
				idQueries += "ids[]=" + i + "&";
			}
			
			String jsonStr = requestData("beatmaps" + idQueries);
			System.out.println(jsonStr);
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonObject().get("beatmaps").getAsJsonArray());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return Optional.empty();}
	}
	

	/**
	 * Gets all of a user's scores on a beatmap.
	 * @param beatmapId
	 * @param userId
	 * @return Optional<JsonArray> - List of Osu API v2 "Score" Structure.
	 */
	public Optional<JsonArray> getBeatmapScoresByUserId(int beatmapId, int userId)
	{
		try
		{
			String jsonStr = requestData("beatmaps/" + beatmapId + "/scores/users/" + userId + "/all");
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonObject().get("scores").getAsJsonArray());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return Optional.empty();}
	}
	
	
	/**
	 * Returns 100 top plays of a specified user.
	 * @param userId
	 * @param scoreType: can be "best" "firsts" or "recent"
	 * @return Optional<JsonArray> - List of osu! API v2 "Score" Structure.
	 */
	public Optional<JsonArray> getTopPlaysByUserId(int userId, String scoreType, int limit)
	{
		try
		{
			String jsonStr = requestData(String.format(
					"users/%s/scores/%s?mode=osu&limit=%d",
					userId, scoreType, limit));
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonArray());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return Optional.empty();}
	}
	
	
	/**
	 * Returns a score, given its score ID.
	 * @param scoreId
	 * @return Optional<JsonObject> - osu! API v2 "Score" Structure.
	 */
	public Optional<JsonObject> getScoreById(String scoreId)
	{
		try
		{
			String jsonStr = requestData(String.format(
					"scores/%s",
					scoreId));
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonObject());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {return Optional.empty();}
	}
	
	
	/**
	 * Returns a user, given its user ID
	 * @param userId
	 * @return Optional<JsonObject> = osu! API v2 "UserExtended" Structure.
	 */
	public Optional<JsonObject> getUserById(long userId)
	{
		try
		{
			String jsonStr = requestData(String.format(
					"users/%d/osu",
					userId));
			
			return Optional.of(JsonParser.parseString(jsonStr).getAsJsonObject());
		}
		catch (JsonParseException e) {return Optional.empty();}
		catch (IOException | InterruptedException e) {return Optional.empty();}
	}
}

