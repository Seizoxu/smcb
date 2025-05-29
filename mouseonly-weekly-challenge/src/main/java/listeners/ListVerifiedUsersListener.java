package listeners;

import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import dataStructures.OsuPlayer;
import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ListVerifiedUsersListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("list-unverified-users"))
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
		
		Optional<List<OsuPlayer>> requestPlayerList = requestUnverifiedPlayerList(event);
		if (requestPlayerList.isEmpty())
		{
			return;
		}
		List<OsuPlayer> playerList = requestPlayerList.get();
		
		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setTitle("Unverified Users")
				.setDescription(playerList.stream()
						.map(player -> String.format("`%d:` %s%n", player.getUserId(), player.getUsername()))
						.collect(Collectors.joining()))
				.build())
		.queue();
	}
	
	
	private static Optional<List<OsuPlayer>> requestUnverifiedPlayerList(SlashCommandInteractionEvent event)
	{
		try
		{
			List<OsuPlayer> request = BotConfig.mowcDb.getUserDao().getUnverifiedUsers();
			return Optional.of(request);
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
				sendFailed(event, "Error: Unknown SQL error.");
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return Optional.empty();
			}
		}
		catch (Exception e)
		{
			sendFailed(event, "Error: Unknown error.");
			System.err.println(String.format("[ERROR] Exception | %s%n", Instant.now().toString()));
			e.printStackTrace();
			return Optional.empty();
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
