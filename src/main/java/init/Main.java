package init;
import listeners.Ping;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main
{
	private static String BOT_TOKEN;

	public static void main(String[] args) throws InterruptedException
	{
		BOT_TOKEN = args[0];

		JDA jda = JDABuilder.createDefault(BOT_TOKEN)
				.addEventListeners(
						new ReadyListener(),
						new Ping())
				.build();
		
		// Wait until JDA is ready; can finish other tasks above in the meanwhile.
		jda.awaitReady();
	}
}
