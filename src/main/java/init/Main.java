package init;
import listeners.Ping;
import listeners.SubmitScoreWeeklyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main
{
	public static void main(String[] args) throws InterruptedException
	{
		final String BOT_TOKEN = args[0];
		final String OSU_CLIENT_ID = args[1];
		final String OSU_CLIENT_SECRET = args[2];
		final String OSU_LEGACY_TOKEN = args[3];

		JDA jda = JDABuilder.createDefault(BOT_TOKEN)
				.addEventListeners(
						new ReadyListener(),
						new Ping(),
						new SubmitScoreWeeklyListener())
				.build();
		
		BotConfig.initialiseOsuApi(OSU_CLIENT_ID, OSU_CLIENT_SECRET, OSU_LEGACY_TOKEN);

		// Wait until JDA is ready; can finish other tasks above in the meanwhile.
		jda.awaitReady();
	}
}
