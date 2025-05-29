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
				.addOption(OptionType.STRING, "link", "An osu! score link.", true),

				Commands.slash("remove-score", "[Admin CMD] Removes a score.")
				.addOption(OptionType.STRING, "link", "An osu! score link/id.", true),

				Commands.slash("add-map", "[Admin CMD] Submits an osu! beatmap to the Mouse-Only Weekly Challenge Database.")
				.addOption(OptionType.STRING, "link", "An osu! beatmap link/id. Must be a beatmap, and not beatmapSET.", true)
				.addOption(OptionType.STRING, "end-date", "An SQL date, formatted as YYYY-MM-DD", true),

				Commands.slash("remove-map", "[Admin CMD] Removes an osu! beatmap from the Mouse-Only Weekly Challenge Database.")
				.addOption(OptionType.STRING, "link", "An osu! beatmap link/id. Must be a beatmap, and not beatmapSET.", true),

				Commands.slash("list-maps", "Lists osu! beatmaps from the present Mouse-Only Weekly Challenge.")
				.addOption(OptionType.STRING, "end-date", "Get maps that end on a certain date.", false),

				Commands.slash("list-unverified-users", "[Admin CMD] Lists unverified users."),

				Commands.slash("verify-user", "[Admin CMD] Verifies a user, so their scores may be counted.")
				.addOption(OptionType.INTEGER, "user-id", "ID of the user to verify", true),

				Commands.slash("unverify-user", "[Admin CMD] Unverifies a user, so their scores may not be counted.")
				.addOption(OptionType.INTEGER, "user-id", "ID of the user to unverify.", true)
				).queue();
	}
}
