package listeners;

import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.time.Instant;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class VerifyToggleListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		Boolean isVerify = switch(event.getName())
		{
			case "verify-user" -> true;
			case "unverify-user" -> false;
			default -> null;
		};
		if (isVerify == null)
		{
			return;
		}
		event.deferReply().queue();

		if (event.getUser().getIdLong() != BotConfig.ADMIN_DISCORD_ID)
		{
			event.getHook().sendMessageEmbeds(new EmbedBuilder()
					.setDescription("Insufficient privileges... lowly peasant.")
					.build())
			.queue();
			return;
		}
		
		long userId = event.getOption("user-id").getAsLong();
		
		boolean isSuccess = changeVerificationStatus(event, userId, isVerify);
		if (isSuccess)
		{
			event.getHook().sendMessageEmbeds(new EmbedBuilder()
					.setDescription(String.format("User `%d` %s.", userId, (isVerify) ? "verified" : "unverified"))
					.build())
			.queue();
		}
	}
	

	private boolean changeVerificationStatus(SlashCommandInteractionEvent event, long userId, boolean verify)
	{
		try
		{
			if (verify)
			{
				BotConfig.mowcDb.getUserDao().verifyUser(userId);
				return true;
			}
			else
			{
				BotConfig.mowcDb.getUserDao().unverifyUser(userId);
				return true;
			}
		}
		catch (UnableToExecuteStatementException e)
		{
			//TODO: Copy this to all classes. Actually, just check all DB errors in one class later.
			Throwable cause = e.getCause() != null ? e.getCause() : e;

			if (cause instanceof SQLSyntaxErrorException)
			{
				sendFailed(event, "SQL Syntax Error.", verify, userId);
				System.err.println(String.format("[ERROR] SQLSyntaxErrorException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return false;
			}
			else if (cause instanceof SQLTimeoutException)
			{
				sendFailed(event, "Unable to send query.", verify, userId);
				System.err.println(String.format("[ERROR] SQLTimeoutException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return false;
			}
			else
			{
				sendFailed(event, "Unknown error.", verify, userId);
				System.err.println(String.format("[ERROR] SQLException | %s%n", Instant.now().toString()));
				cause.printStackTrace();
				return false;
			}
		}
		catch (Exception e)
		{
			sendFailed(event, "Unknown error.", verify, userId);
			System.err.println(String.format("[ERROR] Exception | %s%n", Instant.now().toString()));
			e.printStackTrace();
			return false;
		}
	}
	

	/**
	 * Sends a failure message.
	 * @param message
	 */
	private static void sendFailed(SlashCommandInteractionEvent event, String message, boolean verify, long userId)
	{
		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setDescription(String.format("Unable to %s user `%d:` %s", (verify) ? "verify" : "unverify", userId, message))
				.build())
		.queue();
	}
}
