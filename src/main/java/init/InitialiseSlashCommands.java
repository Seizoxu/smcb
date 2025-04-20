package init;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class InitialiseSlashCommands
{
	public static void init(JDA jda)
	{
		jda.updateCommands().addCommands(
				Commands.slash("submit-score-weekly", "Submits an osu! score to the Mouse-Only Weekly Challenge Sheet.")
				.addOption(OptionType.STRING, "link", "An osu! score link.", true)
				// Add further commands here if required
				).queue();
	}
}
