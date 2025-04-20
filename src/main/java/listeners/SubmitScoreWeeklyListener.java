package listeners;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import init.BotConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SubmitScoreWeeklyListener extends ListenerAdapter
{
	private static SlashCommandInteractionEvent event;
	private static List<String> allowedMods = List.of(
			"EZ", "NF", "HT",
			"HR", "SD", "PF", "DT", "NC", "HD", "FL",
			"SO", "CL");


	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if (!event.getName().equals("submit-score-weekly"))
		{
			return;
		}

		this.event = event;

		// Received reply, send "we are processing."
		event.deferReply().queue();
		
		// Get score ID and retrieve Score object.
		String link = event.getOption("link").getAsString();
		Optional<JsonObject> scoreResponse = retrieveScore(link);
		if (!scoreResponse.isPresent())
		{
			return;
		}
		
		// Retrieve beatmap ID and user ID.
		JsonObject scoreData = scoreResponse.get();
		System.out.println(scoreData);
		int beatmapId = scoreData.getAsJsonObject("beatmap").get("id").getAsInt();
		int userId = scoreData.get("user_id").getAsInt();
		int totalScore = scoreData.get("total_score").getAsInt();
		Instant timestamp = Instant.parse(scoreData.get("ended_at").getAsString());
		List<JsonElement> modsData = scoreData.get("mods").getAsJsonArray().asList();
		
		// Only allow stable mods.
		List<String> modsList = modsData.stream()
				.map(jsElem -> jsElem.getAsJsonObject().get("acronym").getAsString())
				.toList();
		List<String> invalidMods = modsList.stream()
				.filter(mod -> !allowedMods.contains(mod))
				.toList();
		if (!invalidMods.isEmpty())
		{
			sendFailed("Mod(s) not allowed: " + String.join(", ", invalidMods));
			return;
		}
		
		// Remove CL debuff.
		if (modsList.contains("CL"))
		{
			totalScore = (int)Math.round((double)totalScore/0.96);
		}
		

		event.getHook().sendMessageFormat(
				"Beatmap ID: %d%n"
				+ "User ID: %d%n"
				+ "Total Score: %d%n"
				+ "Mods: %s%n"
				+ "Timestamp: %s%n",
				beatmapId,
				userId,
				totalScore,
				modsList.stream().collect(Collectors.joining(", ")),
				timestamp)
		.queue();
	}
	

	/**
	 * Retrieves osu! API v2 "Score" Structure from a given score link.
	 * @param link - Must be of the new score system in the format of "https://osu.ppy.sh/scores/:id"
	 * @return
	 */
	private static Optional<JsonObject> retrieveScore(String link)
	{
		Matcher matcher = Pattern.compile("^(https?:\\/\\/)?osu\\.ppy\\.sh\\/scores\\/(\\d+)$").matcher(link);
		if (!matcher.matches())
		{
			sendFailed("Invalid score link: link format rejected.");
			return Optional.empty();
		}
		
		Optional<JsonObject> response = BotConfig.osuApi.getScoreById(matcher.group(2));
		if (!response.isPresent())
		{
			sendFailed("Unable to retrieve score data.");
			return Optional.empty();
		}
		
		if (response.get().has("error"))
		{
			sendFailed(String.format("Invalid score link: API error - `%s`", response.get().get("error")));
			return Optional.empty();
		}
		
		return response;
	}
	

	/**
	 * Sends a failure message.
	 * @param message
	 */
	private static void sendFailed(String message)
	{
		event.getHook().sendMessage(message).queue();
	}
}
