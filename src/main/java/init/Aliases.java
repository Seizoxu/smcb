package init;

import java.util.Collections;
import java.util.Set;

public class Aliases
{
	public static final Set<String> PING = Collections.singleton(BotConfig.PREFIX + "ping");
	public static final Set<String> EXAMPLE = Set.of(
			BotConfig.PREFIX + "",
			BotConfig.PREFIX + "");
}
