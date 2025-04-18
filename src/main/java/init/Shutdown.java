package init;

import net.dv8tion.jda.api.JDA;
import okhttp3.OkHttpClient;

public class Shutdown
{
	public static void shutdown(JDA jda, boolean forceShutdown)
	{
		OkHttpClient client = jda.getHttpClient();
		client.connectionPool().evictAll();
		client.dispatcher().executorService().shutdown();
		
		if (forceShutdown)
		{
			jda.shutdownNow();
		}
		else
		{
			jda.shutdown();
		}
	}
}
