package listeners;

import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import dataStructures.OsuMap;
import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RemoveMapsListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("remove-map") || event.getUser().getIdLong() != BotConfig.ADMIN_DISCORD_ID)
		{
			return;
		}
		event.deferReply().queue();
		
		OsuMap map;
		try
		{
			String link = event.getOption("link").getAsString();
			Optional<OsuMap> mapDeleteRequest = retrieveAndDeleteBeatmap(event, link);
			if (mapDeleteRequest.isEmpty())
			{
				return;
			}
			
			map = mapDeleteRequest.get();
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
			else
			{
				sendFailed(event, "Error: Unknown error.");
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return;
			}
		}

		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setTitle("Removed Map")
				.setDescription(String.format("`%d | %s - %s [%s] (%s)`",
						map.getMapId(), map.getArtist(), map.getTitle(), map.getDifficultyName(), map.getMapper()))
				.build())
				.queue();
	}
	
	
	private static Optional<OsuMap> retrieveAndDeleteBeatmap(SlashCommandInteractionEvent event, String link)
	{
		// Retrieve map ID from link.
		Matcher matcher = Pattern.compile("(?:https://)?(?:osu\\.ppy\\.sh/(?:beatmapsets/\\d+#osu/|b/))?(\\d+)$").matcher(link);
		if (!matcher.matches())
		{
			sendFailed(event, "Invalid map link: link format rejected.");
			return Optional.empty();
		}

		// Request map from DB, then delete.
		int mapId = Integer.parseInt(matcher.group(1));
		OsuMap map = BotConfig.mowcDb.getMapDao().getMap(mapId);
		BotConfig.mowcDb.getMapDao().removeMap(mapId);
		
		return Optional.of(map);
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
