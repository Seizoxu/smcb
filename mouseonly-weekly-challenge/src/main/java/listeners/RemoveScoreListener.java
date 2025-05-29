package listeners;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RemoveScoreListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("remove-score"))
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
		
		String link = event.getOption("link").getAsString();
		Optional<Long> parsedId = retrieveScoreId(event, link);
		if (parsedId.isEmpty())
		{
			return;
		}
		
		long scoreId = parsedId.get();
		
		requestDeleteByScoreId(event, scoreId);
	}


	/**
	 * Retrieves the score ID from a score link
	 * @param link - Must be of the new score system in the format of "https://osu.ppy.sh/scores/:id";
	 * protocol and domain are optional -- id is required at the bare minimum.
	 * @return
	 */
	private static Optional<Long> retrieveScoreId(SlashCommandInteractionEvent event, String link)
	{
		Matcher matcher = Pattern.compile("^(?:https:\\/\\/)?osu\\.ppy\\.sh\\/scores\\/(\\d+)$|^(\\d+)$").matcher(link);
		if (!matcher.matches())
		{
			sendFailed(event, "Invalid score link: link format rejected");
			return Optional.empty();
		}

		try
		{
			long scoreId = Long.parseLong(
					(matcher.group(1) != null)
					? matcher.group(1)
					: matcher.group(2));
			return Optional.of(scoreId);
		}
		catch (IllegalStateException | IndexOutOfBoundsException | NumberFormatException e)
		{
			sendFailed(event, "Invalid score link: couldn't parse score ID.");
			System.err.println(e.getLocalizedMessage());
			return Optional.empty();
		}
	}
	
	
	private static void requestDeleteByScoreId(SlashCommandInteractionEvent event, long scoreId)
	{
		try
		{
			BotConfig.mowcDb.getScoreDao().deleteScoreByScoreId(scoreId);
			
			event.getHook().sendMessageEmbeds(new EmbedBuilder()
					.setDescription(String.format("Score [%d](<https://osu.ppy.sh/scores/%d>) deleted.", scoreId, scoreId))
					.build())
			.queue();
		}
		catch (UnableToExecuteStatementException e)
		{
			Throwable cause = e.getCause();

			if (cause instanceof SQLIntegrityConstraintViolationException)
			{
				sendFailed(event, String.format("Error: Score entry with ID `%d` is constrained by a foreign key.", scoreId));
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
		catch (Exception e)
		{
			sendFailed(event, "Error: Unknown error.");
			System.err.println(String.format("[ERROR] Exception | %s%n", Instant.now().toString()));
			e.printStackTrace();
			return;
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
