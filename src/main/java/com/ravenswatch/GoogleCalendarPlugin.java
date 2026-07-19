package com.ravenswatch;

import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.task.Schedule;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
		name = "Raven's Watch Clan",
		description = "Raven's Watch Clan Info",
		tags = {"calendar", "schedule", "events"}
)
public class GoogleCalendarPlugin extends Plugin
{
	@Inject private ClientToolbar clientToolbar;
	@Inject private GoogleCalendarConfig config;
	@Inject private GoogleCalendarClient calendarClient;

	private GoogleCalendarPanel panel;
	private NavigationButton navButton;

	@Provides
	GoogleCalendarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GoogleCalendarConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = new GoogleCalendarPanel();

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/calendar_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Raven's Watch Clan")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		refreshCalendar();
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Schedule(
			period = 15,
			unit = ChronoUnit.MINUTES,
			asynchronous = true
	)
	public void fetchCalendarUpdates()
	{
		refreshCalendar();
	}

	private void refreshCalendar()
	{
		if (!config.apiKey().isEmpty() && !config.calendarId().isEmpty()) {
			calendarClient.fetchEvents(config.apiKey(), config.calendarId(), new GoogleCalendarClient.CalendarCallback() {
				@Override
				public void onSuccess(GoogleCalendarClient.CalendarResponse response) {
					List<String[]> processedEvents = new ArrayList<>();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
					ZoneId localZone = ZoneId.systemDefault();

					if (response != null && response.items != null) {
						for (GoogleCalendarClient.CalendarEvent event : response.items) {
							// Check if it's an all-day event or missing a specific timestamp
							if (event.start == null || event.start.dateTime == null) {
								continue;
							}

							// 1. Clean up the event title text (removes unsupported emojis/boxes)
							String titleText = event.summary != null ? event.summary : "Untitled Event";
							String cleanTitle = titleText.replaceAll("[^a-zA-Z0-9\\s\\p{Punct}]", "").trim();

							// 2. Convert from server timezone to local timezone, then format
							ZonedDateTime localTime = ZonedDateTime.parse(event.start.dateTime)
									.withZoneSameInstant(localZone);
							String cleanDateText = localTime.format(formatter);

							processedEvents.add(new String[]{cleanTitle, cleanDateText});
						}
					}

					panel.updateEventsList(processedEvents);
				}

				@Override
				public void onError(String error) {
					// Fail silently in the UI background
				}
			});
		}
	}
}
