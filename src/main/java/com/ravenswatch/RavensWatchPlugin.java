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
import net.runelite.api.events.ChatMessage;
import okhttp3.*;
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
		description = "Raven's Watch Clan Info and Broadcasts",
		tags = {"calendar", "schedule", "events", "drops", "webhook"}
)
public class RavensWatchPlugin extends Plugin
{
	@Inject private ClientToolbar clientToolbar;
	@Inject private RavensWatchConfig config;
	@Inject private RavensWatchClient calendarClient;
	@Inject private Notifier notifier;
	@Inject private Client client;
	@Inject private OkHttpClient okHttpClient;

	private RavensWatchPanel panel;
	private NavigationButton navButton;

	private final Set<String> notifiedEvents = new HashSet<>();
	private List<EventAlarmData> activeEvents = new ArrayList<>();

	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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
		refreshMotm();
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("ravenswatch"))
		{
			refreshCalendar();
			refreshMotm();
		}
	}

	private void refreshMotm()
	{
		String motmUrl = "https://gist.githubusercontent.com/Hayley2709/3d693f4116914d3877be79a67f2a402a/raw/39b59a5dee052c51cd9a208201694b9ae4612562/gistfile1.txt";

// Pass motmUrl directly into your HTTP client/request builder here

		{
			calendarClient.fetchMotm("https://gist.githubusercontent.com/Hayley2709/3d693f4116914d3877be79a67f2a402a/raw/90fe4a91b32e6d191dbca98b7206513f44e5b573/ravenswatch-motm.json", new RavensWatchClient.MotmCallback() {
				@Override
				public void onSuccess(RavensWatchClient.MotmResponse response) {
					if (panel != null && response != null) {
						panel.updateMotmDisplay(response.name, response.reason);
					}
				}
				@Override
				public void onError(String error) {}
			});
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.enableDropLogger() || config.clanWebhookUrl().isEmpty())
		{
			return;
		}

		String message = event.getMessage();
		String sender = event.getName();

		if (isBroadcastableMessage(message))
		{
			sendDiscordEmbedWebhook(config.clanWebhookUrl(), sender, message);
		}
	}

	private boolean isBroadcastableMessage(String message)
	{
		String lower = message.toLowerCase();
		return lower.contains("valuable drop:") ||
				lower.contains("received a drop:") ||
				lower.contains("pet") ||
				lower.contains("collection log unlocked");
	}

	private void sendDiscordEmbedWebhook(String webhookUrl, String playerName, String dropText)
	{
		String user = (playerName != null && !playerName.isEmpty()) ? playerName : "Clan Member";

		String jsonPayload = "{" +
				"\"username\": \"Raven's Watch Bot\"," +
				"\"embeds\": [{" +
				"\"title\": \"🎉 " + user + " scored a drop!\"," +
				"\"description\": \"" + dropText.replace("\"", "\\\"") + "\"," +
				"\"color\": 3447003" +
				"}]" +
				"}";

		RequestBody body = RequestBody.create(JSON, jsonPayload);
		Request request = new Request.Builder().url(webhookUrl).post(body).build();

		okHttpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, java.io.IOException e) {}
			@Override
			public void onResponse(Call call, Response response) { response.close(); }
		});
	}

	@Schedule(period = 1, unit = ChronoUnit.MINUTES, asynchronous = true)
	public void fetchCalendarUpdates()
	{
		refreshCalendar();
		refreshMotm();
	}

	@Schedule(period = 30, unit = ChronoUnit.SECONDS, asynchronous = true)
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

				long secondsUntil = ChronoUnit.SECONDS.between(now, event.startTime);
				long thresholdSeconds = alertMinutes * 60L;

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

							String uniqueId = cleanTitle + "_" + localTime.toEpochSecond();
							newAlarmEvents.add(new EventAlarmData(cleanTitle, uniqueId, localTime));
						}
					}

					synchronized (activeEvents) {
						activeEvents.clear();
						activeEvents.addAll(newAlarmEvents);
						notifiedEvents.retainAll(activeEvents.stream().map(e -> e.uniqueId).collect(java.util.stream.Collectors.toList()));
					}

					panel.updateEventsList(processedEvents);
				}

				@Override
				public void onError(String error) {}
			});
		}
	}
}
