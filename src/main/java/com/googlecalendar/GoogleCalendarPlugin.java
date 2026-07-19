package com.googlecalendar;

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

@PluginDescriptor(
		name = "Ravens Watch Calendar",
		description = "Displays upcoming Google Calendar events in the sidebar",
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

		// Loads our custom icon from the plugin resources folder
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/calendar_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Ravens Watch Calendar") // Fixed the sidebar hover tooltip name
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

	/**
	 * Automatically runs every 15 minutes to pull the latest calendar updates.
	 * RuneLite's scheduler handles this safely off the main game engine thread.
	 */
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
					panel.updateEvents(response);
				}

				@Override
				public void onError(String error) {
					// Fail silently in the UI background
				}
			});
		}
	}
}
