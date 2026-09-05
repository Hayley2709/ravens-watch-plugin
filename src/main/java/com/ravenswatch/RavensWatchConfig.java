package com.ravenswatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("ravenswatch")
public interface RavensWatchConfig extends Config
{
	@ConfigItem(
			keyName = "pluginPassword",
			name = "Access Key",
			description = "Enter the clan key to unlock panel content",
			secret = true,
			position = 0
	)
	default String pluginPassword()
	{
		return "";
	}

	@ConfigItem(
			keyName = "apiKey",
			name = "Google Calendar API Key",
			description = "Your Google Calendar API key for fetching events",
			position = 1
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
			keyName = "calendarId",
			name = "Calendar ID",
			description = "The Google Calendar ID (e.g., email address or public ID)",
			position = 2
	)
	default String calendarId()
	{
		return "";
	}

	@ConfigItem(
			keyName = "upcomingMonthOnly",
			name = "Upcoming Month Only",
			description = "Only display events scheduled within the next month",
			position = 3
	)
	default boolean upcomingMonthOnly()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enableNotifications",
			name = "Enable Event Notifications",
			description = "Receive desktop and chat reminders before clan events start",
			position = 4
	)
	default boolean enableNotifications()
	{
		return true;
	}

	@ConfigItem(
			keyName = "minimumDropValue",
			name = "Minimum Drop Value",
			description = "Minimum GP value required to log a valuable drop broadcast.",
			position = 3
	)
	default int minimumDropValue()
	{
		return 100000; // Default threshold (e.g., 50k GP)
	}

	@Range(min = 1, max = 60)
	@ConfigItem(
			keyName = "notificationMinutes",
			name = "Notification Minutes",
			description = "How many minutes before an event to trigger a reminder",
			position = 5
	)
	default int notificationMinutes()
	{
		return 15;
	}

	@ConfigItem(
			keyName = "enableDropLogger",
			name = "Enable Clan Drop Logger",
			description = "Log valuable clan drops and milestones to Discord",
			position = 6
	)
	default boolean enableDropLogger()
	{
		return true;
	}

	@ConfigItem(
			keyName = "clanWebhookUrl",
			name = "Discord Webhook URL",
			description = "The Discord webhook URL where clan drop embeds will be posted",
			position = 7
	)
	default String clanWebhookUrl()
	{
		return "";
	}
}