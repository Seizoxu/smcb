package init;
import listeners.Ping;
import listeners.SubmitScoreWeeklyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main
{
	public static void main(String[] args) throws InterruptedException
	{
		BotConfig.initialise();

		JDA jda = JDABuilder.createDefault(BotConfig.BOT_TOKEN)
				.addEventListeners(
						new ReadyListener(),
						new Ping(),
						new SubmitScoreWeeklyListener())
				.build();
		
		// Wait until JDA is ready; can finish other tasks above in the meanwhile.
		jda.awaitReady();
	}
}
