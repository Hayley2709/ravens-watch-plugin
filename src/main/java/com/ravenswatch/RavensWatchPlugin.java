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
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PluginDescriptor(
		name = "Raven's Watch Clan",
		description = "Raven's Watch Clan Info",
		tags = {"calendar", "schedule", "events"}
)
public class RavensWatchPlugin extends Plugin
{
	@Inject private ClientToolbar clientToolbar;
	@Inject private RavensWatchConfig config;
	@Inject private RavensWatchClient calendarClient;
	@Inject private Notifier notifier;
	@Inject private Client client;

	private RavensWatchPanel panel;
	private NavigationButton navButton;

	// Track event IDs/summaries that have already triggered an alert to avoid spamming
	private final Set<String> notifiedEvents = new HashSet<>();
	// Keep a local reference of parsed events for the alarm ticker
	private List<EventAlarmData> activeEvents = new ArrayList<>();

	// Simple wrapper class to hold parsed timestamp data for the notification loop
	private static class EventAlarmData {
		String title;
		String uniqueId;
		ZonedDateTime startTime;

		EventAlarmData(String title, String uniqueId, ZonedDateTime startTime) {
			this.title = title;
			this.uniqueId = uniqueId;
			this.startTime = startTime;
		}
	}

	@Provides
	RavensWatchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RavensWatchConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = new RavensWatchPanel();

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

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
// Check if the change belongs to your config group
		if (event.getGroup().equals("ravenswatch"))
		{
// Whenever any config item (like upcomingMonthOnly) changes, refresh immediately
			refreshCalendar();
		}
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

	// Runs every 30 seconds to check if any cached event is about to start
	@Schedule(
			period = 30,
			unit = ChronoUnit.SECONDS,
			asynchronous = true
	)
	public void checkEventAlarms()
	{
		if (!config.enableNotifications() || activeEvents.isEmpty()) {
			return;
		}

		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		int alertMinutes = config.notificationMinutes();

		synchronized (activeEvents) {
			for (EventAlarmData event : activeEvents) {
				if (notifiedEvents.contains(event.uniqueId)) {
					continue;
				}

// Calculate time remaining until event starts
				long secondsUntil = ChronoUnit.SECONDS.between(now, event.startTime);
				long thresholdSeconds = alertMinutes * 60L;

// If we are inside the countdown window and the event hasn't already passed
				if (secondsUntil > 0 && secondsUntil <= thresholdSeconds) {
					triggerAlert(event.title, alertMinutes);
					notifiedEvents.add(event.uniqueId);
				}
			}
		}
	}

	private void triggerAlert(String title, int minutes) {
		String message = "Clan Event '" + title + "' starting in about " + minutes + " minutes!";

		notifier.notify(message);

// Safely send chat message if client is logged in
		if (client != null && client.getGameState() == net.runelite.api.GameState.LOGGED_IN) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
	}

	private void refreshCalendar()
	{
		if (!config.apiKey().isEmpty() && !config.calendarId().isEmpty()) {
			calendarClient.fetchEvents(config.apiKey(), config.calendarId(), new RavensWatchClient.CalendarCallback() {
				@Override
				public void onSuccess(RavensWatchClient.CalendarResponse response) {
					List<String[]> processedEvents = new ArrayList<>();
					List<EventAlarmData> newAlarmEvents = new ArrayList<>();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
					ZoneId localZone = ZoneId.systemDefault();

					ZonedDateTime now = ZonedDateTime.now(localZone);
					ZonedDateTime oneMonthFromNow = now.plusMonths(1);

					if (response != null && response.items != null) {
						for (RavensWatchClient.CalendarEvent event : response.items) {
							if (event.start == null || event.start.dateTime == null) {
								continue;
							}

							ZonedDateTime localTime = ZonedDateTime.parse(event.start.dateTime)
									.withZoneSameInstant(localZone);

							if (config.upcomingMonthOnly()) {
								if (localTime.isBefore(now) || localTime.isAfter(oneMonthFromNow)) {
									continue;
								}
							} else {
								if (localTime.isBefore(now)) {
									continue;
								}
							}

							String titleText = event.summary != null ? event.summary : "Untitled Event";
							String cleanTitle = titleText.replaceAll("[^a-zA-Z0-9\\s\\p{Punct}]", "").trim();
							String cleanDateText = localTime.format(formatter);

							processedEvents.add(new String[]{cleanTitle, cleanDateText});

// Create a reliable unique key based on the title and timestamp
							String uniqueId = cleanTitle + "_" + localTime.toEpochSecond();
							newAlarmEvents.add(new EventAlarmData(cleanTitle, uniqueId, localTime));
						}
					}

// Update local alarm tracking list safely
					synchronized (activeEvents) {
						activeEvents.clear();
						activeEvents.addAll(newAlarmEvents);
// Clear notification history using Java 11 compatible collector
						notifiedEvents.retainAll(activeEvents.stream().map(e -> e.uniqueId).collect(java.util.stream.Collectors.toList()));
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