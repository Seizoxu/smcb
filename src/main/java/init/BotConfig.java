package init;

import io.github.cdimascio.dotenv.Dotenv;
import wrappers.OsuWrapper;

public class BotConfig
{
	public static final String PREFIX = "\\";
	public static OsuWrapper osuApi;
	private static final Dotenv DOTENV = Dotenv.configure()
			.directory("./")
			.ignoreIfMalformed()
			.ignoreIfMissing()
			.load();
	
	private static final String OSU_CLIENT_ID = getVar("OSU_CLIENT_ID");
	private static final String OSU_CLIENT_SECRET = getVar("OSU_CLIENT_SECRET");
	private static final String OSU_LEGACY_TOKEN = getVar("OSU_LEGACY_TOKEN");
	protected static final String BOT_TOKEN = getVar("BOT_TOKEN");
	public static final String DB_IP = getVar("DB_IP");
	public static final String DB_USER = getVar("DB_USER");
	public static final String DB_PASSWORD = getVar("DB_PASSWORD");
	public static final String GSHEETS_ID = getVar("GSHEETS_ID");

	public static void initialise()
	{
		try
		{
			osuApi = new OsuWrapper(OSU_CLIENT_ID, OSU_CLIENT_SECRET, OSU_LEGACY_TOKEN);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getVar(String key)
	{
		return DOTENV.get(key);
	}
}
