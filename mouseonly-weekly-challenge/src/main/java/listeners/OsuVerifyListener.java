package listeners;

import init.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OsuVerifyListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("osuverify"))
		{
			return;
		}
		event.deferReply(true).queue();

		sendAuthLink(event);
	}
	
	
	private static void sendAuthLink(SlashCommandInteractionEvent event)
	{
		try
		{
			long discordId = event.getUser().getIdLong();

			event.getHook().sendMessageEmbeds(new EmbedBuilder()
					.setTitle("Authentication Link")
					.setDescription(String.format(
							"[Click me.](<https://osu.ppy.sh/oauth/authorize?client_id=%s&response_type=code&scope=%s&state=%d>)",
							BotConfig.OSU_CLIENT_ID, "identify", discordId))
					.build())
			.queue();
		}
		catch(Exception e)
		{
			sendFailed(event, "Unknown error while requesting authentication token.");
			System.err.println("[ERROR] Unknown exception while requesting authentication token: " + e.getLocalizedMessage());
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
