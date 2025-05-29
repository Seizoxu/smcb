package init;
import listeners.AddMapsListener;
import listeners.ListMapsListener;
import listeners.ListVerifiedUsersListener;
import listeners.Ping;
import listeners.RemoveMapsListener;
import listeners.RemoveScoreListener;
import listeners.SubmitScoreWeeklyListener;
import listeners.VerifyToggleListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main
{
	public static void main(String[] args) throws InterruptedException
	{
		BotConfig.initialise();

		JDA jda = JDABuilder.createDefault(BotConfig.BOT_TOKEN)
				.addEventListeners(
						new ReadyListener(),
						new Ping(),
						new SubmitScoreWeeklyListener(),
						new AddMapsListener(),
						new RemoveMapsListener(),
						new ListMapsListener(),
						new RemoveScoreListener(),
						new ListVerifiedUsersListener(),
						new VerifyToggleListener())
				.enableIntents(GatewayIntent.MESSAGE_CONTENT)
				.build();
		
		// Wait until JDA is ready; can finish other tasks above in the meanwhile.
		jda.awaitReady();
	}
}
