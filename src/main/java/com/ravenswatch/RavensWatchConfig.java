package com.ravenswatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ravenswatch")
public interface RavensWatchConfig extends Config {

	@ConfigItem(
			keyName = "apiKey",
			name = "API Key",
			description = "Your Google Cloud Calendar API Key. Please visit the Clan Discord server to find out how to generate this."
	)
	default String apiKey() {
		return "";
	}

	@ConfigItem(
			keyName = "calendarId",
			name = "Calendar ID",
			description = "The Google Calendar ID. Please visit the Clan Discord server for instructions on how to find this."
	)
	default String calendarId() {
		return "";
	}

	@ConfigItem(
			keyName = "upcomingMonthOnly",
			name = "Upcoming Month Only",
			description = "Only display events happening within the next 30 days",
			position = 1
	)
	default boolean upcomingMonthOnly() {
		return true;
	}


}
