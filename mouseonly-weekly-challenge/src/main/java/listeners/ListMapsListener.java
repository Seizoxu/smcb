package listeners;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import dataStructures.OsuMap;
import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ListMapsListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("list-maps"))
		{
			return;
		}
		event.deferReply().queue();
		
		// Check if endDate exists.
		String endDate;
		try
		{
			endDate = event.getOption("end-date").getAsString();
		}
		catch (NullPointerException e)
		{
			endDate = null;
		}
		
		// Get maps.
		List<OsuMap> maps;
		try
		{
			maps = (endDate == null)
					? BotConfig.mowcDb.getMapDao().getAllMaps()
					: BotConfig.mowcDb.getMapDao().getMapsOfEndDate(endDate);
		}
		catch (UnableToExecuteStatementException e)
		{
			sendFailed(event, String.format("Error: SQLException%n```%n%s%n```", e.getLocalizedMessage()));
			System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
			e.printStackTrace();
			return;
		}
		catch (Exception e)
		{
			sendFailed(event, String.format("Error: Unknown Exception:%n```%n%s%n```", e.getLocalizedMessage()));
			System.err.println(String.format("[ERROR] Exception | %s%n", Instant.now().toString()));
			e.printStackTrace();
			return;
		}
		
		// Send maps.
		//TODO: Limit to 20, and paginate.
		DateTimeFormatter sqlDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setTitle("Maps")
				.setDescription(
						maps.stream()
						.filter(map -> map.getEndDate() != null && !map.getEndDate().isEmpty())
						.sorted(Comparator.comparing(
								(OsuMap map) -> LocalDate.parse(map.getEndDate(), sqlDateFormat))
								.reversed())
						.limit(20)
						.map(map -> String.format("`%d` | %s - %s [%s] (%s)", map.getMapId(), map.getArtist(), map.getTitle(), map.getDifficultyName(), map.getMapper()))
						.collect(Collectors.joining("\n")))
				.build())
		.queue();
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
