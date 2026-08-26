package com.ravenswatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ravenswatch")
public interface RavensWatchConfig extends Config {

	@ConfigItem(
			keyName = "apiKey",
			name = "API Key",
			description = "Your Google Cloud Calendar API Key. Please visit the Clan Discord server to find out how to generate this.",
			position = 1
	)
	default String apiKey() {
		return "";
	}

	@ConfigItem(
			keyName = "calendarId",
			name = "Calendar ID",
			description = "The Google Calendar ID. Please visit the Clan Discord server for instructions on how to find this.",
			position = 2
	)
	default String calendarId() {
		return "";
	}

	@ConfigItem(
			keyName = "upcomingMonthOnly",
			name = "Upcoming Month Only",
			description = "Only display events happening within the next 30 days",
			position = 3
	)
	default boolean upcomingMonthOnly() {
		return true;
	}

	@ConfigItem(
			keyName = "enableNotifications",
			name = "Event Notifications",
			description = "Enable alerts before scheduled clan events start",
			position = 4
	)
	default boolean enableNotifications() {
		return true;
	}

	@ConfigItem(
			keyName = "notificationMinutes",
			name = "Alert Time (Minutes)",
			description = "How many minutes before an event to trigger the notification",
			position = 5
	)
	default int notificationMinutes() {
		return 10;
	}
}