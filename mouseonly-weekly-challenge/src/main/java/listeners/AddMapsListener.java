package listeners;

import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import com.google.gson.JsonObject;

import dataStructures.OsuMap;
import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AddMapsListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("add-map"))
		{
			return;
		}
		if (event.getUser().getIdLong() != BotConfig.ADMIN_DISCORD_ID)
		{
			event.getHook().sendMessageEmbeds(new EmbedBuilder()
					.setDescription("Insufficient privileges... lowly peasant.")
					.build())
			.queue();
			return;
		}
		event.deferReply().queue();
		
		// parse link and get map
		String link = event.getOption("link").getAsString();
		Optional<OsuMap> mapRequest = retrieveBeatmap(event, link);
		if (mapRequest.isEmpty())
		{
			return;
		}
		OsuMap map = mapRequest.get();
		
		// add map to db
		try
		{
			BotConfig.mowcDb.getMapDao().insertMap(map.getMapId(), map.getMapsetId(), map.getEndDate(), map.getTitle(), map.getArtist(),
					map.getMapper(), map.getDifficultyName(), map.getBannerLink(), map.getStarRating(), map.getAr(), map.getOd(), map.getHp(),
					map.getCs(), map.getLengthSeconds(), map.getBpm());
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLSyntaxErrorException)
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
			else if (cause instanceof SQLException)
			{
				sendFailed(event, String.format("Error: SQLException%n```%n%s%n```", cause.getLocalizedMessage()));
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
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

		// send response
		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setTitle("Submitted Map")
				.setDescription(String.format("`%d | %s - %s [%s] (%s)`",
						map.getMapId(), map.getArtist(), map.getTitle(), map.getDifficultyName(), map.getMapper()))
				.build()).queue();
	}
	
	
	private static Optional<OsuMap> retrieveBeatmap(SlashCommandInteractionEvent event, String link)
	{
		// Retrieve map ID from link.
		Matcher matcher = Pattern.compile("(?:https://)?(?:osu\\.ppy\\.sh/(?:beatmapsets/\\d+#osu/|b/))?(\\d+)$").matcher(link);
		if (!matcher.matches())
		{
			sendFailed(event, "Invalid map link: link format rejected.");
			return Optional.empty();
		}

		// Request osu! API.
		int mapId = Integer.parseInt(matcher.group(1));
		Optional<JsonObject> response = BotConfig.osuApi.getBeatmapById(mapId);
		if (response.isEmpty())
		{
			sendFailed(event, "Unable to retrieve map data.");
			return Optional.empty();
		}
		else if (response.get().has("error"))
		{
			sendFailed(event, String.format("Invalid map link: API error - `%s`", response.get().get("error")));
			return Optional.empty();
		}
		
		// Parse and return.
		JsonObject map = response.get();
		int mapsetId = map.get("beatmapset_id").getAsInt();
		String endDate = event.getOption("end-date").getAsString().strip();
		String title = map.get("beatmapset").getAsJsonObject().get("title").getAsString();
		String artist = map.get("beatmapset").getAsJsonObject().get("artist").getAsString();
		String mapper = map.get("beatmapset").getAsJsonObject().get("creator").getAsString();
		String difficultyName = map.get("version").getAsString();
		double starRating = map.get("difficulty_rating").getAsDouble();
		double ar = map.get("ar").getAsDouble();
		double od = map.get("accuracy").getAsDouble();
		double hp = map.get("drain").getAsDouble();
		double cs = map.get("cs").getAsDouble();
		int lengthSeconds = map.get("total_length").getAsInt();
		int bpm = map.get("bpm").getAsInt();
		String bannerLink = map.get("beatmapset").getAsJsonObject().get("covers").getAsJsonObject().get("cover@2x").getAsString();
		
		return Optional.of(new OsuMap(mapId, mapsetId, endDate, title, artist, mapper, difficultyName, bannerLink, starRating, ar, od, hp, cs, lengthSeconds, bpm));
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
