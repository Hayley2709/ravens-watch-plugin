package com.ravenswatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("googlecalendar")
public interface GoogleCalendarConfig extends Config
{
	@ConfigItem(
			keyName = "apiKey",
			name = "API Key",
			description = "Your Google Cloud Calendar API Key"
	)
	default String apiKey() { return ""; }

	@ConfigItem(
			keyName = "calendarId",
			name = "Calendar ID",
			description = "The Google Calendar ID (e.g., your email or a public calendar string)"
	)
	default String calendarId() { return ""; }
}
