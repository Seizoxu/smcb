package listeners;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dataStructures.OsuMap;
import dataStructures.OsuPlayer;
import dataStructures.OsuScore;
import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SubmitRecentScoreListener extends ListenerAdapter
{
	private static List<String> allowedMods = List.of(
			"EZ", "NF", "HT",
			"HR", "SD", "PF", "DT", "NC", "HD", "FL",
			"SO", "CL");


	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("submit-recent-score"))
		{
			return;
		}
		event.deferReply().queue();

		// Get OsuPlayer from Discord ID
		long discordId = event.getUser().getIdLong();
		Optional<OsuPlayer> userRequest = getPlayerFromDiscordId(event, discordId);
		if (userRequest.isEmpty())
		{
			return;
		}
		OsuPlayer user = userRequest.get();
		
		// Get OsuScore from OsuPlayer
		Optional<OsuScore> scoreRequest = getRecentScoreFromPlayer(event, user);
		if (scoreRequest.isEmpty())
		{
			return;
		}
		OsuScore score = scoreRequest.get();

		// Validate and get OsuMap.
		Optional<OsuMap> mapRequest = validateAndGetMap(event, score);
		if(mapRequest.isEmpty())
		{
			return;
		}
		OsuMap map = mapRequest.get();
		
		// Send score to MOWC DB.
		boolean submissionSuccess = submitScore(event, score);
		if (!submissionSuccess)
		{
			return;
		}
		
		// Send score submission success
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setTitle("Score Submitted!")
				.setDescription(String.format(
						  "`Beatmap    :` %d | %s [%s]%n"
						+ "`User ID    :` %d (%s)%n"
						+ "`Total Score:` %s%n"
						+ "`Mods       :` %s%n"
						+ "`Timestamp  :` %s%n",
						map.getMapId(), map.getTitle(), map.getDifficultyName(),
						score.getUserId(), user.getUsername(),
						nf.format(score.getScore()),
						Arrays.stream(score.getMods()).collect(Collectors.joining(",")),
						score.getTimestamp())
						)
				.build())
		.queue();
	}
	
	
	/**
	 * Retrieves an osu! API v2 "UserExtended" Structure simplified into an OsuPlayer structure, from a Discord ID.
	 * @param event
	 * @param discordId
	 * @return Optional<OsuPlayer>
	 */
	private static Optional<OsuPlayer> getPlayerFromDiscordId(SlashCommandInteractionEvent event, long discordId)
	{
		try
		{
			OsuPlayer playerRequest = BotConfig.mowcDb.getUserDao().getUserFromDiscordId(discordId);
			if (playerRequest.getDiscordId() == -1)
			{
				sendFailed(event, "Unable to retrieve player data. Either the database is down, or you have not registered. Please link your account with /osuset.");
				return Optional.empty();
			}
			return Optional.of(playerRequest);
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLSyntaxErrorException)
			{
				sendFailed(event, "Error: SQL Syntax Error.");
				System.err.println(String.format("[ERROR] SQLSyntaxErrorException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
			else if (cause instanceof SQLTimeoutException)
			{
				sendFailed(event, "Error: Unable to send query.");
				System.err.println(String.format("[ERROR] SQLTimeoutException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
			else
			{
				sendFailed(event, "Error: Unknown error.");
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
		}
	}


	/**
	 * Retrieves osu! API v2 "Score" Structure from a given OsuPlayer
	 * @param link - Must be of the new score system in the format of "https://osu.ppy.sh/scores/:id"
	 * @return Optional<OsuScore>
	 */
	private static Optional<OsuScore> getRecentScoreFromPlayer(SlashCommandInteractionEvent event, OsuPlayer player)
	{
		// get player's >rs
		Optional<JsonArray> recentScoreRequest = BotConfig.osuApi.getTopPlaysByUserId(player.getUserId(), "recent", 1);
		if (recentScoreRequest.isEmpty())
		{
			sendFailed(event, "Unable to retrieve score data.");
			return Optional.empty();
		}
		else if (recentScoreRequest.get().getAsJsonArray().isEmpty())
		{
			sendFailed(event, String.format("Player [%s](<https://osu.ppy/sh/u/%d>) has no recent scores.", player.getUsername(), player.getUserId()));
			return Optional.empty();
		}
		else if (recentScoreRequest.get().getAsJsonArray().get(0).getAsJsonObject().has("error"))
		{
			sendFailed(event, String.format("Invalid score link: API error - `%s`", recentScoreRequest.get().getAsJsonObject().has("error")));
			return Optional.empty();
		}
		JsonObject scoreData = recentScoreRequest.get().getAsJsonArray().get(0).getAsJsonObject();
		
		// Retrieve beatmap ID and user ID.
		long scoreId = scoreData.get("id").getAsLong();
		long userId = scoreData.get("user_id").getAsLong();
		int beatmapId = scoreData.getAsJsonObject("beatmap").get("id").getAsInt();
		int totalScore = scoreData.get("total_score").getAsInt();
		List<JsonElement> modsData = scoreData.get("mods").getAsJsonArray().asList();
		Instant timestamp = Instant.parse(scoreData.get("ended_at").getAsString());
		
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

		// Add EZ multiplier
		//TODO: make this configurable later.
		if (modsList.contains("EZ"))
		{
			totalScore = (int)Math.round((double)totalScore*1.8);
		}
		
		return Optional.of(new OsuScore(scoreId, userId, beatmapId, totalScore, modsList.toArray(new String[0]), timestamp));
	}
	
	
	/**
	 * Validates the existence of a map and/or the submission window of the score. Retrieves the map if valid, returns empty if not.
	 * @param event
	 * @param score
	 * @return Optional<OsuMap>
	 */
	private static Optional<OsuMap> validateAndGetMap(SlashCommandInteractionEvent event, OsuScore score)
	{
		try
		{
			Optional<OsuMap> mapRequest = BotConfig.mowcDb.getMapDao().getMap(score.getMapId());
			if (mapRequest.isEmpty())
			{
				sendFailed(event, String.format(
						"Error: Submitted score ID [%d](<https://osu.ppy.sh/scores/%d>) that refers to map ID [%d](<https://osu.ppy.sh/b/%d>) is not in the weekly map list",
						score.getScoreId(), score.getScoreId(), score.getMapId(), score.getMapId()));
			}

			boolean isMapValid = BotConfig.mowcDb.getMapDao().isMapInSubmissionWindow(score.getMapId(),Timestamp.from(score.getTimestamp())).isPresent();
			if (!isMapValid)
			{
				sendFailed(event, String.format("Error: Submitted score with score ID [%d](<https://osu.ppy.sh/scores/%d>) is not in the submission window.",
						score.getScoreId(), score.getScoreId()));
				return Optional.empty();
			}
			
			return mapRequest;
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLIntegrityConstraintViolationException)
			{
				sendFailed(event, String.format("SQL Error: Integrity constraint violation on submitted score for map ID [%d](<https://osu.ppy.sh/b/%d>).",
						score.getMapId(), score.getMapId()));
				System.err.println(String.format("[ERROR] SQLIntegrityConstraintViolationException | %s%n", Instant.now().toString()));
				return Optional.empty();
			}
			else if (cause instanceof SQLSyntaxErrorException)
			{
				sendFailed(event, "Error: SQL Syntax Error.");
				System.err.println(String.format("[ERROR] SQLSyntaxErrorException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
			else if (cause instanceof SQLTimeoutException)
			{
				sendFailed(event, "Error: Unable to send query.");
				System.err.println(String.format("[ERROR] SQLTimeoutException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
			else
			{
				sendFailed(event, "Error: Unknown error.");
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
		}
	}
	
	
	/**
	 * Submits a score to the MOWC database.
	 * @param event
	 * @param score
	 * @return boolean
	 */
	private static boolean submitScore(SlashCommandInteractionEvent event, OsuScore score)
	{
		try
		{
			BotConfig.mowcDb.getScoreDao().callInsertOrUpdateScoreIfHigher(score.getScoreId(), score.getUserId(), score.getMapId(),
					score.getScore(), String.join(",", score.getMods()), Timestamp.from(score.getTimestamp()));
			return true;
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLSyntaxErrorException)
			{
				sendFailed(event, "Error: SQL Syntax Error.");
				System.err.println(String.format("[ERROR] SQLSyntaxErrorException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return false;
			}
			else if (cause instanceof SQLTimeoutException)
			{
				sendFailed(event, "Error: Unable to send query.");
				System.err.println(String.format("[ERROR] SQLTimeoutException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return false;
			}
			else
			{
				sendFailed(event, "Error: Unknown error.");
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return false;
			}
		}
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
