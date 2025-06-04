package listeners;

import init.InitialiseSlashCommands;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UpdateCommandsListener extends ListenerAdapter
{
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("update-commands"))
		{
			return;
		}
		event.deferReply().queue();
		
		InitialiseSlashCommands.init(event.getJDA());
		
		event.getHook().sendMessageEmbeds(new EmbedBuilder()
				.setTitle("Updated commands.")
				.build())
		.queue();
	}
}
