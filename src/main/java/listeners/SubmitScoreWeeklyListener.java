package listeners;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dataStructures.OsuPlayer;
import dataStructures.OsuScore;
import init.BotConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SubmitScoreWeeklyListener extends ListenerAdapter
{
	private static List<String> allowedMods = List.of(
			"EZ", "NF", "HT",
			"HR", "SD", "PF", "DT", "NC", "HD", "FL",
			"SO", "CL");


	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("submit-score-weekly"))
		{
			return;
		}

		// Received reply, send "we are processing."
		event.deferReply().queue();
		
		// Get score ID and retrieve Score object.
		String link = event.getOption("link").getAsString();
		Optional<OsuScore> scoreResponse = retrieveScore(event, link);
		if (scoreResponse.isEmpty())
		{
			return;
		}
		OsuScore score = scoreResponse.get();

		// Send score to MOWC DB.
		try
		{
			boolean userExists = BotConfig.mowcDb.getUserDao().userExists(score.getUserId());
			if (!userExists)
			{
				Optional<OsuPlayer> userResponse = retrieveUser(event, score.getUserId());
				if (userResponse.isEmpty())
				{
					return;
				}
				OsuPlayer user = userResponse.get();
				
				BotConfig.mowcDb.getUserDao().insertUser(user.getUserId(), user.getUsername(), user.getCountryCode(), user.isVerified());
			}
			
			BotConfig.mowcDb.getScoreDao().insertOrUpdateScore(score.getScoreId(), score.getUserId(), score.getMapId(),
					score.getScore(), Arrays.stream(score.getMods()).collect(Collectors.joining(",")), score.getTimestamp());
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLIntegrityConstraintViolationException)
			{
				sendFailed(event, String.format("Error: Submitted score with map ID %d is not in the weekly map list.", score.getScoreId()));
				return;
			}
			else if (cause instanceof SQLSyntaxErrorException)
			{
				sendFailed(event, "Error: SQL Syntax Error.");
				System.err.println(String.format("[ERROR] SQLSyntaxErrorException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return;
			}
			else if (cause instanceof SQLTimeoutException)
			{
				sendFailed(event, "Error: Unable to send query.");
				System.err.println(String.format("[ERROR] SQLTimeoutException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return;
			}
			else
			{
				sendFailed(event, "Error: Unknown error.");
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return;
			}
		}
		
		// Send score submission success
		event.getHook().sendMessageFormat(
				"Score submitted!%n"
				+ "Beatmap ID: %d%n"
				+ "User ID: %d%n"
				+ "Total Score: %d%n"
				+ "Mods: %s%n"
				+ "Timestamp: %s%n",
				score.getMapId(),
				score.getUserId(),
				score.getScore(),
				Arrays.stream(score.getMods()).collect(Collectors.joining(",")),
				score.getTimestamp())
		.queue();
		//TODO: make command to show list of unverified users, and change their verified status
		//TODO: retrieve scores from db and send to gsheets
	}
	

	/**
	 * Retrieves osu! API v2 "Score" Structure from a given score link.
	 * @param link - Must be of the new score system in the format of "https://osu.ppy.sh/scores/:id"
	 * @return
	 */
	private static Optional<OsuScore> retrieveScore(SlashCommandInteractionEvent event, String link)
	{
		Matcher matcher = Pattern.compile("^(https?:\\/\\/)?osu\\.ppy\\.sh\\/scores\\/(\\d+)$").matcher(link);
		if (!matcher.matches())
		{
			sendFailed(event, "Invalid score link: link format rejected.");
			return Optional.empty();
		}
		
		long scoreId = Long.parseLong(matcher.group(2));
		Optional<JsonObject> response = BotConfig.osuApi.getScoreById(String.valueOf(scoreId));
		if (response.isEmpty())
		{
			sendFailed(event, "Unable to retrieve score data.");
			return Optional.empty();
		}
		else if (response.get().has("error"))
		{
			sendFailed(event, String.format("Invalid score link: API error - `%s`", response.get().get("error")));
			return Optional.empty();
		}
		
		// Retrieve beatmap ID and user ID.
		JsonObject scoreData = response.get();
		int beatmapId = scoreData.getAsJsonObject("beatmap").get("id").getAsInt();
		int userId = scoreData.get("user_id").getAsInt();
		int totalScore = scoreData.get("total_score").getAsInt();
		Instant timestamp = Instant.parse(scoreData.get("ended_at").getAsString());
		List<JsonElement> modsData = scoreData.get("mods").getAsJsonArray().asList();
		
		// Only allow stable mods.
		List<String> modsList = modsData.stream()
				.map(jsElem -> jsElem.getAsJsonObject().get("acronym").getAsString())
				.toList();
		List<String> invalidMods = modsList.stream()
				.filter(mod -> !allowedMods.contains(mod))
				.toList();
		if (!invalidMods.isEmpty())
		{
			sendFailed(event, "Mod(s) not allowed: " + String.join(", ", invalidMods));
			return Optional.empty();
		}
		
		// Remove CL debuff.
		if (modsList.contains("CL"))
		{
			totalScore = (int)Math.round((double)totalScore/0.96);
		}
		
		return Optional.of(new OsuScore(scoreId, userId, beatmapId, totalScore, modsList.toArray(new String[0]), timestamp));
	}
	
	
	/**
	 * Retrieves an osu! API v2 "UserExtended" Structure simplified into an OsuPlayer structure.
	 * @param event
	 * @param userId
	 * @return Optional<OsuPlayer>
	 */
	private static Optional<OsuPlayer> retrieveUser(SlashCommandInteractionEvent event, long userId)
	{
		Optional<JsonObject> response = BotConfig.osuApi.getUserById(userId);
		if (response.isEmpty())
		{
			sendFailed(event, "Unable to retrieve score data.");
			return Optional.empty();
		}
		else if (response.get().has("error"))
		{
			sendFailed(event, String.format("Invalid score link: API error - `%s`", response.get().get("error")));
			return Optional.empty();
		}
		
		JsonObject playerData = response.get();
		String username = playerData.get("username").getAsString();
		String countryCode = playerData.get("country").getAsJsonObject().get("code").getAsString();
		
		return Optional.of(new OsuPlayer(userId, username, countryCode, false));
	}
	

	/**
	 * Sends a failure message.
	 * @param message
	 */
	private static void sendFailed(SlashCommandInteractionEvent event, String message)
	{
		event.getHook().sendMessage(message).queue();
	}
}
