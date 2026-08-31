package com.ravenswatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("ravenswatch")
public interface RavensWatchConfig extends Config
{
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
			keyName = "womGroupId",
			name = "Wise Old Man Group ID",
			description = "The Wise Old Man group ID for your clan",
			position = 3
	)
	default int womGroupId()
	{
		return 10344;
	}

	@ConfigItem(
			keyName = "womApiKey",
			name = "Wise Old Man API Key",
			description = "API key used when querying Wise Old Man endpoints",
			position = 4
	)
	default String womApiKey()
	{
		return "q2e861suwzqhzpp6qy3nc3wm";
	}

	@ConfigItem(
			keyName = "upcomingMonthOnly",
			name = "Upcoming Month Only",
			description = "Only display events scheduled within the next month",
			position = 5
	)
	default boolean upcomingMonthOnly()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enableNotifications",
			name = "Enable Event Notifications",
			description = "Receive desktop and chat reminders before clan events start",
			position = 6
	)
	default boolean enableNotifications()
	{
		return true;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(
			keyName = "notificationMinutes",
			name = "Notification Minutes",
			description = "How many minutes before an event to trigger a reminder",
			position = 7
	)
	default int notificationMinutes()
	{
		return 15;
	}

	@ConfigItem(
			keyName = "enableDropLogger",
			name = "Enable Clan Drop Logger",
			description = "Log valuable clan drops and milestones to Discord",
			position = 8
	)
	default boolean enableDropLogger()
	{
		return true;
	}

	@ConfigItem(
			keyName = "clanWebhookUrl",
			name = "Discord Webhook URL",
			description = "The Discord webhook URL where clan drop embeds will be posted",
			position = 9
	)
	default String clanWebhookUrl()
	{
		return "";
	}
}