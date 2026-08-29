package com.ravenswatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.*;

@Slf4j
@PluginDescriptor(
        name = "Raven's Watch Clan",
        description = "Raven's Watch Clan Info and Broadcasts",
        tags = {"calendar", "schedule", "events", "drops", "webhook", "wise old man"}
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

    private static final String[] OSRS_SKILLS = {
            "Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft",
            "Construction", "Hitpoints", "Agility", "Herblore", "Thieving", "Crafting", "Fletching",
            "Slayer", "Hunter", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking",
            "Woodcutting", "Farming", "Sailing"
    };

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
        panel = new RavensWatchPanel(this);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/Images/RW_Plugin_icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Raven's Watch Clan")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        refreshCalendar();
        refreshMotm();
        fetchWiseOldManMemberCount();
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
            fetchWiseOldManMemberCount();
        }
    }

    private void refreshMotm()
    {
        String motmApiUrl = "https://api.github.com/gists/3d693f4116914d3877be79a67f2a402a";

        calendarClient.fetchMotm(motmApiUrl, new RavensWatchClient.MotmCallback() {
            @Override
            public void onSuccess(List<String> ravensList) {
                if (panel != null) {
                    panel.updateGoldenRavensDisplay(ravensList);
                }
            }

            @Override
            public void onError(String error) {
                log.error("MOTM Fetch Failed: {}", error);
            }
        });
    }

    private void fetchWiseOldManMemberCount()
    {
        HttpUrl url = HttpUrl.parse("https://api.wiseoldman.net/v2/groups/10344");
        if (url == null) return;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .header("x-api-key", "q2e861suwzqhzpp6qy3nc3wm")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (panel != null) {
                    SwingUtilities.invokeLater(() -> panel.setMemberCount("Total Members: Unavailable"));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (panel != null) {
                        SwingUtilities.invokeLater(() -> panel.setMemberCount("Total Members: Unavailable"));
                    }
                    response.close();
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = new JsonParser().parse(responseBody).getAsJsonObject();

                    int memberCount = 0;
                    if (jsonObject.has("memberCount")) {
                        memberCount = jsonObject.get("memberCount").getAsInt();
                    } else if (jsonObject.has("members") && jsonObject.get("members").isJsonArray()) {
                        memberCount = jsonObject.getAsJsonArray("members").size();
                    }

                    int finalCount = memberCount;
                    if (panel != null) {
                        SwingUtilities.invokeLater(() -> panel.setMemberCount("Total Members: " + finalCount));
                    }
                } catch (Exception e) {
                    if (panel != null) {
                        SwingUtilities.invokeLater(() -> panel.setMemberCount("Total Members: Error"));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    public void simulateTestBroadcast()
    {
        String mockMessage = "TestPlayer received a drop: Twisted bow (1)";

        // 1. Update the UI panel immediately
        if (panel != null)
        {
            panel.addRecentDrop(mockMessage);
        }

        // 2. Test Discord webhook integration if configured
        if (!config.clanWebhookUrl().isEmpty())
        {
            sendDiscordEmbedWebhook(config.clanWebhookUrl(), mockMessage);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        // Quick test trigger command in game chat
        if (event.getType() == ChatMessageType.PUBLICCHAT && event.getMessage().equalsIgnoreCase("::testdrop"))
        {
            simulateTestBroadcast();
            return;
        }

        if (!config.enableDropLogger() || config.clanWebhookUrl().isEmpty())
        {
            return;
        }

        switch (event.getType())
        {
            case CLAN_MESSAGE:
            case CLAN_GIM_MESSAGE:
                String message = event.getMessage();
                if (isBroadcastableMessage(message) && isLocalPlayerEvent(message))
                {
                    sendDiscordEmbedWebhook(config.clanWebhookUrl(), message);

                    String cleanMessage = message.replaceAll("<[^>]*>", "").trim();
                    if (panel != null)
                    {
                        panel.addRecentDrop(cleanMessage);
                    }
                }
                break;
            default:
                break;
        }
    }

    private boolean isLocalPlayerEvent(String message)
    {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
        {
            return false;
        }

        String localName = client.getLocalPlayer().getName().replace('\u00A0', ' ').trim();
        String cleanMessage = message.replaceAll("<[^>]*>", "").replace('\u00A0', ' ').trim();

        return cleanMessage.toLowerCase().startsWith(localName.toLowerCase());
    }

    private boolean isBroadcastableMessage(String message)
    {
        String lower = message.toLowerCase();

        return lower.contains("valuable drop:") ||
                lower.contains("received a drop:") ||
                lower.contains("feels like you're being followed") ||
                lower.contains("collection log") ||
                lower.contains("has achieved level") ||
                lower.contains("has reached level") ||
                lower.contains("total level of") ||
                lower.contains("xp in") ||
                lower.contains("has completed") ||
                lower.contains("personal best:");
    }

    public void sendDiscordEmbedWebhook(String webhookUrl, String rawMessage)
    {
        String cleanMessage = rawMessage.replaceAll("<[^>]*>", "").trim();
        String lower = cleanMessage.toLowerCase();

        String title = "📢 Clan Announcement";
        String thumbnailUrl = null;

        if (lower.contains("received a drop:") || lower.contains("valuable drop:")) {
            title = "🎉 New High Value drop!";
            thumbnailUrl = extractItemImageUrl(cleanMessage);
        } else if (lower.contains("has completed a quest") || lower.contains("completed the quest")) {
            title = "🎉 New quest completed!";
            thumbnailUrl = "https://oldschool.runescape.wiki/images/Quest_point_icon.png";
        } else if (lower.contains("xp in")) {
            title = "🎉 New XP Milestone reached!";
            thumbnailUrl = extractSkillImageUrl(cleanMessage);
        } else if (lower.contains("has achieved level") || lower.contains("has reached level") || lower.contains("total level")) {
            title = "🎉 New Level Milestone reached!";
            thumbnailUrl = extractSkillImageUrl(cleanMessage);
        } else if (lower.contains("followed") || lower.contains("pet")) {
            title = "🎉 New Pet unlocked!";
            thumbnailUrl = "https://oldschool.runescape.wiki/images/Pet_icon.png";
        } else if (lower.contains("collection log")) {
            title = "🎉 New Collection Log entry!";
            thumbnailUrl = "https://oldschool.runescape.wiki/images/Collection_log.png";
        } else if (lower.contains("has completed")) {
            title = "🎉 New Achievement completed!";
            thumbnailUrl = "https://oldschool.runescape.wiki/images/Combat_Achievements.png";
        } else if (lower.contains("personal best")) {
            title = "🎉 New Personal Best!";
            thumbnailUrl = "https://oldschool.runescape.wiki/images/Timer.png";
        }

        StringBuilder json = new StringBuilder();
        json.append("{")
                .append("\"username\": \"Raven's Watch Bot\",")
                .append("\"embeds\": [{")
                .append("\"title\": \"").append(title).append("\",")
                .append("\"description\": \"").append(cleanMessage.replace("\"", "\\\"")).append("\",")
                .append("\"color\": 3447003");

        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            json.append(",\"thumbnail\": {\"url\": \"").append(thumbnailUrl).append("\"}");
        }

        json.append("}]}");

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder().url(webhookUrl).post(body).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) { response.close(); }
        });
    }

    private String extractItemImageUrl(String message)
    {
        String lower = message.toLowerCase();
        String itemName = null;

        if (lower.contains("received a drop:")) {
            int start = message.indexOf("received a drop:") + "received a drop:".length();
            int end = message.indexOf("(", start);
            if (end == -1) end = message.indexOf(".", start);
            if (end != -1) {
                itemName = message.substring(start, end).trim();
            }
        } else if (lower.contains("valuable drop:")) {
            int start = message.indexOf("valuable drop:") + "valuable drop:".length();
            int end = message.indexOf("(", start);
            if (end == -1) end = message.indexOf(".", start);
            if (end != -1) {
                itemName = message.substring(start, end).trim();
            }
        }

        if (itemName != null && !itemName.isEmpty()) {
            String formatted = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
            formatted = formatted.replace(" ", "_");
            return "https://oldschool.runescape.wiki/images/" + formatted + "_detail.png";
        }

        return "https://oldschool.runescape.wiki/images/Coins_detail.png";
    }

    private String extractSkillImageUrl(String message)
    {
        String lower = message.toLowerCase();
        for (String skill : OSRS_SKILLS) {
            if (lower.contains(" in " + skill.toLowerCase()) || lower.contains(" " + skill.toLowerCase() + " level")) {
                return "https://oldschool.runescape.wiki/images/" + skill + "_icon.png";
            }
        }
        return "https://oldschool.runescape.wiki/images/Stats_icon.png";
    }

    @Schedule(period = 1, unit = ChronoUnit.MINUTES, asynchronous = true)
    public void fetchCalendarUpdates()
    {
        refreshCalendar();
        refreshMotm();
        fetchWiseOldManMemberCount();
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
                        notifiedEvents.retainAll(activeEvents.stream().map(e -> e.uniqueId).collect(Collectors.toList()));
                    }

                    if (panel != null) {
                        SwingUtilities.invokeLater(() -> panel.updateEventsList(processedEvents));
                    }
                }

                @Override
                public void onError(String error) {}
            });
        }
    }
}