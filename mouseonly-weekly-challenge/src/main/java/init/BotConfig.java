package init;

import io.github.cdimascio.dotenv.Dotenv;
import wrappers.DatabaseWrapper;
import wrappers.OsuWrapper;

public class BotConfig
{
	public static final String PREFIX = "\\";
	public static OsuWrapper osuApi;
	public static DatabaseWrapper mowcDb;
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
	public static final long ADMIN_DISCORD_ID = Long.parseLong(getVar("ADMIN_DISCORD_ID"));
	
	private static final int DB_CONNECTION_RETRIES = 10;
	private static final int DB_CONNECTION_RETRY_DELAY_MS = 3000;

	public static void initialise()
	{
		try
		{
			osuApi = new OsuWrapper(OSU_CLIENT_ID, OSU_CLIENT_SECRET, OSU_LEGACY_TOKEN);
			
			for (int i=0; i<DB_CONNECTION_RETRIES; i++)
			{
				try
				{
					mowcDb = new DatabaseWrapper(String.format("jdbc:mysql://%s/mowc", DB_IP), DB_USER, DB_PASSWORD);
					mowcDb.createTables();
					System.out.println("[INIT] Database connected successfully.");
					break;
				}
				catch (Exception e)
				{
					System.err.println("[ERROR] Failed to connect to DB, attempt " + (i + 1));
					
					if (i == DB_CONNECTION_RETRIES-1)
					{
						e.printStackTrace();
						throw new RuntimeException("[ERROR] Failed to connect to DB after " + (i+1) + " attempts.");
					}
					
					try
					{
						Thread.sleep(DB_CONNECTION_RETRY_DELAY_MS);
					}
					catch (InterruptedException ie) {}
				}
			}
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
