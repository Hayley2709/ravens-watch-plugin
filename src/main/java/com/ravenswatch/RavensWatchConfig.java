package com.ravenswatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("ravenswatch")
public interface RavensWatchConfig extends Config {

	@ConfigSection(
			name = "Calendar & Alarms",
			description = "Google Calendar sync and event notifications",
			position = 1
	)
	String calendarSection = "calendarSection";

	@ConfigSection(
			name = "Discord Integration",
			description = "Drop logger and clan broadcast settings",
			position = 2
	)
	String webhookSection = "webhookSection";

	// --- Calendar & Alarms ---
	@ConfigItem(
			keyName = "apiKey",
			name = "API Key",
			description = "Your Google Cloud Calendar API Key.",
			section = calendarSection,
			position = 1
	)
	default String apiKey() {
		return "";
	}

	@ConfigItem(
			keyName = "calendarId",
			name = "Calendar ID",
			description = "The Google Calendar ID.",
			section = calendarSection,
			position = 2
	)
	default String calendarId() {
		return "";
	}

	@ConfigItem(
			keyName = "upcomingMonthOnly",
			name = "Upcoming Month Only",
			description = "Only display events happening within the next 30 days",
			section = calendarSection,
			position = 3
	)
	default boolean upcomingMonthOnly() {
		return true;
	}

	@ConfigItem(
			keyName = "enableNotifications",
			name = "Event Notifications",
			description = "Enable alerts before scheduled clan events start",
			section = calendarSection,
			position = 4
	)
	default boolean enableNotifications() {
		return true;
	}

	@ConfigItem(
			keyName = "notificationMinutes",
			name = "Alert Time (Minutes)",
			description = "How many minutes before an event to trigger the notification",
			section = calendarSection,
			position = 5
	)
	default int notificationMinutes() {
		return 10;
	}

	// --- Webhooks & Integration ---
	@ConfigItem(
			keyName = "clanWebhookUrl",
			name = "Clan Discord Webhook",
			description = "Paste your Discord channel webhook URL for drops and broadcasts",
			section = webhookSection,
			position = 6
	)
	default String clanWebhookUrl() {
		return "";
	}

	@ConfigItem(
			keyName = "enableDropLogger",
			name = "Enable Drop Logger",
			description = "Broadcast valuable drops to the clan Discord channel",
			section = webhookSection,
			position = 7
	)
	default boolean enableDropLogger() {
		return true;
	}
}