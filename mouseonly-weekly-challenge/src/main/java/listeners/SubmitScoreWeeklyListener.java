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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dataStructures.OsuMap;
import dataStructures.OsuPlayer;
import dataStructures.OsuScore;
import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
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
		OsuPlayer user = null;
		OsuMap map = null;
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
				user = userResponse.get();
				
				BotConfig.mowcDb.getUserDao().insertUser(user.getUserId(), user.getUsername(), user.getCountryCode(), user.isVerified(), user.getDiscordId());
			}
			
			user = BotConfig.mowcDb.getUserDao().getUsername(score.getUserId());

			boolean isInWindow = BotConfig.mowcDb.getMapDao().isMapInSubmissionWindow(score.getMapId(),Timestamp.from(score.getTimestamp())).isPresent();
			if (!isInWindow)
			{
				sendFailed(event, String.format("Error: Submitted score with score ID [%d](<https://osu.ppy.sh/scores/%d>) is not in the submission window.",
						score.getScoreId(), score.getScoreId()));
				return;
			}
			
			Optional<OsuMap> mapRequest = BotConfig.mowcDb.getMapDao().getMap(score.getMapId());
			if (mapRequest.isEmpty())
			{
				sendFailed(event, String.format("Error: Map with map ID [%d](<https://osu.ppy.sh/b/%d>) does not exist in DB.", score.getMapId(), score.getMapId()));
				return;
			}
			map = mapRequest.get();
			
			BotConfig.mowcDb.getScoreDao().callInsertOrUpdateScoreIfHigher(score.getScoreId(), score.getUserId(), score.getMapId(),
					score.getScore(), String.join(",", score.getMods()), Timestamp.from(score.getTimestamp()));
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLIntegrityConstraintViolationException)
			{
				sendFailed(event, String.format("Error: Submitted score with score ID %d is not in the weekly map list.", score.getScoreId()));
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

		// Add EZ multiplier
		//TODO: make this configurable later.
		if (modsList.contains("EZ"))
		{
			totalScore = (int)Math.round((double)totalScore*1.8);
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
		
		return Optional.of(new OsuPlayer(userId, username, countryCode, false, -1));
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
