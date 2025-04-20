package init;

import wrappers.OsuWrapper;

public class BotConfig
{
	public static final String PREFIX = "\\";
	public static OsuWrapper osuApi;
	
	public static void initialiseOsuApi(String clientId, String clientSecret, String legacyToken)
	{
		try
		{
			osuApi = new OsuWrapper(clientId, clientSecret, legacyToken);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
