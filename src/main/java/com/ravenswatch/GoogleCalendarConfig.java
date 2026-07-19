@ConfigItem(
		keyName = "apiKey",
		name = "API Key",
		description = "Your Google Cloud Calendar API Key. Please visit the Clan Discord server to find out how to generate this."
)
default String apiKey() { return ""; }

@ConfigItem(
		keyName = "calendarId",
		name = "Calendar ID",
		description = "The Google Calendar ID. Please visit the Clan Discord server for instructions on how to find this."
)
default String calendarId() { return ""; }
