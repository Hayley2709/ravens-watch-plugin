package com.ravenswatch;

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
        description = "Raven's Watch Clan Info, Competitions, and Broadcasts",
        tags = {"calendar", "schedule", "events", "drops", "webhook", "wise old man", "wom", "competitions"}
)
public class RavensWatchPlugin extends Plugin
{
    private static final String MOTM_GIST_URL = "https://api.github.com/gists/3d693f4116914d3877be79a67f2a402a";
    private static final int WOM_GROUP_ID = 10344;
    private static final String WOM_API_KEY = "q2e861suwzqhzpp6qy3nc3wm";
    private static final String REQUIRED_KEY = "YourSecretClanPasswordHere";

    private static final String WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxmyI8BskO27yqq6v5tVt3pDadKBMaDpVLbJa-NU17oolzIxvDS4333nmmSTkBMI43bVg/exec";

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

        BufferedImage icon;
        try
        {
            icon = ImageUtil.loadImageResource(getClass(), "/Images/RW_Plugin_icon.png");
        }
        catch (IllegalArgumentException e)
        {
            log.warn("Failed to load /Images/RW_Plugin_icon.png resource, using fallback", e);
            icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }

        navButton = NavigationButton.builder()
                .tooltip("Raven's Watch Clan")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        checkAccessState();
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
    }

    private boolean isAccessKeyValid()
    {
        return REQUIRED_KEY.equals(config.pluginPassword());
    }

    private void checkAccessState()
    {
        boolean valid = isAccessKeyValid();
        if (panel != null)
        {
            panel.setPanelUnlocked(valid);
        }

        if (valid)
        {
            fetchAllData();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("ravenswatch"))
        {
            return;
        }

        checkAccessState();
    }

    @Schedule(period = 10, unit = ChronoUnit.MINUTES, asynchronous = true)
    public void fetchAllData()
    {
        if (!isAccessKeyValid()) return;

        fetchGroupMemberCount();
        fetchMotmList();
        fetchCalendarEvents();
        fetchCompetitions();
        fetchBroadcasts();
    }

    private void fetchGroupMemberCount()
    {
        if (WOM_GROUP_ID <= 0)
        {
            panel.setMemberCount("Total Members: N/A");
            return;
        }

        calendarClient.fetchWomMemberCount(WOM_GROUP_ID, WOM_API_KEY, new RavensWatchClient.WomMemberCountCallback()
        {
            @Override
            public void onSuccess(int count)
            {
                panel.setMemberCount("Total Members: " + count);
            }

            @Override
            public void onError(String error)
            {
                log.error("Failed to fetch WOM member count: {}", error);
                panel.setMemberCount("Total Members: Error");
            }
        });
    }

    private void fetchMotmList()
    {
        calendarClient.fetchMotm(MOTM_GIST_URL, new RavensWatchClient.MotmCallback()
        {
            @Override
            public void onSuccess(List<String> ravensList)
            {
                SwingUtilities.invokeLater(() -> panel.updateGoldenRavensDisplay(ravensList));
            }

            @Override
            public void onError(String error)
            {
                log.error("Failed to fetch Golden Ravens list: {}", error);
                SwingUtilities.invokeLater(() -> panel.updateGoldenRavensDisplay(new ArrayList<>()));
            }
        });
    }

    private void fetchCalendarEvents()
    {
        if (config.apiKey().isEmpty() || config.calendarId().isEmpty())
        {
            panel.updateEventsList(new ArrayList<>());
            return;
        }

        calendarClient.fetchEvents(config.apiKey(), config.calendarId(), new RavensWatchClient.CalendarCallback()
        {
            @Override
            public void onSuccess(RavensWatchClient.CalendarResponse response)
            {
                List<String[]> processedEvents = new ArrayList<>();
                List<EventAlarmData> newAlarmEvents = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
                ZoneId localZone = ZoneId.systemDefault();

                ZonedDateTime now = ZonedDateTime.now(localZone);
                ZonedDateTime oneMonthFromNow = now.plusMonths(1);

                if (response != null && response.items != null)
                {
                    for (RavensWatchClient.CalendarEvent event : response.items)
                    {
                        if (event.start == null || event.start.dateTime == null)
                        {
                            continue;
                        }

                        ZonedDateTime localTime = ZonedDateTime.parse(event.start.dateTime)
                                .withZoneSameInstant(localZone);

                        if (config.upcomingMonthOnly())
                        {
                            if (localTime.isBefore(now) || localTime.isAfter(oneMonthFromNow))
                            {
                                continue;
                            }
                        }
                        else
                        {
                            if (localTime.isBefore(now))
                            {
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

                synchronized (activeEvents)
                {
                    activeEvents.clear();
                    activeEvents.addAll(newAlarmEvents);
                    notifiedEvents.retainAll(activeEvents.stream().map(e -> e.uniqueId).collect(Collectors.toList()));
                }

                panel.updateEventsList(processedEvents);
            }

            @Override
            public void onError(String error)
            {
                log.error("Failed to fetch Google Calendar events: {}", error);
            }
        });
    }

    public void fetchCompetitions()
    {
        if (WOM_GROUP_ID <= 0)
        {
            return;
        }

        calendarClient.fetchWomCompetitions(WOM_GROUP_ID, WOM_API_KEY, new RavensWatchClient.WomCompetitionsCallback()
        {
            @Override
            public void onSuccess(List<RavensWatchClient.WomCompetition> competitions)
            {
                if (competitions == null || competitions.isEmpty())
                {
                    panel.updateCompetitionsList(competitions);
                    return;
                }

                List<RavensWatchClient.WomCompetition> activeComps = competitions.stream()
                        .filter(RavensWatchClient.WomCompetition::isActive)
                        .collect(Collectors.toList());

                if (activeComps.isEmpty())
                {
                    panel.updateCompetitionsList(activeComps);
                    return;
                }

                List<RavensWatchClient.WomCompetition> detailedCompetitions = new ArrayList<>();
                int[] remainingRequests = {activeComps.size()};

                for (RavensWatchClient.WomCompetition comp : activeComps)
                {
                    calendarClient.fetchCompetitionDetails(comp.id, WOM_API_KEY, new RavensWatchClient.WomCompetitionDetailsCallback()
                    {
                        @Override
                        public void onSuccess(RavensWatchClient.WomCompetition detailedComp)
                        {
                            synchronized (detailedCompetitions)
                            {
                                detailedCompetitions.add(detailedComp != null ? detailedComp : comp);
                                remainingRequests[0]--;
                                if (remainingRequests[0] == 0)
                                {
                                    panel.updateCompetitionsList(detailedCompetitions);
                                }
                            }
                        }

                        @Override
                        public void onError(String error)
                        {
                            synchronized (detailedCompetitions)
                            {
                                detailedCompetitions.add(comp);
                                remainingRequests[0]--;
                                if (remainingRequests[0] == 0)
                                {
                                    panel.updateCompetitionsList(detailedCompetitions);
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error)
            {
                log.error("Failed to fetch competitions: {}", error);
            }
        });
    }

    private void fetchBroadcasts()
    {
        calendarClient.fetchBroadcasts(new RavensWatchClient.BroadcastsCallback()
        {
            @Override
            public void onSuccess(List<String> broadcasts)
            {
                panel.updateBroadcastsDisplay(broadcasts);
            }

            @Override
            public void onError(String error)
            {
                log.error("Failed to fetch broadcasts: {}", error);
            }
        });
    }

    public void postBroadcastToSheet(String broadcastText)
    {
        JsonObject json = new JsonObject();
        json.addProperty("broadcast", broadcastText);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(WEB_APP_URL)
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.error("Failed to post broadcast to sheet: {}", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                response.close();
            }
        });
    }

    public void simulateTestBroadcast()
    {
        String mockMessage = "TestPlayer received a drop: Twisted bow (1)";

        postBroadcastToSheet(mockMessage);

        if (panel != null)
        {
            panel.addRecentDrop(mockMessage);
        }

        if (!config.clanWebhookUrl().isEmpty())
        {
            sendDiscordEmbedWebhook(config.clanWebhookUrl(), mockMessage);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!isAccessKeyValid())
        {
            return;
        }

        if (event.getType() == ChatMessageType.PUBLICCHAT && event.getMessage().equalsIgnoreCase("::testdrop"))
        {
            simulateTestBroadcast();
            return;
        }

        if (!config.enableDropLogger())
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
                    String cleanMessage = message.replaceAll("<[^>]*>", "").trim();

                    postBroadcastToSheet(cleanMessage);

                    if (!config.clanWebhookUrl().isEmpty())
                    {
                        sendDiscordEmbedWebhook(config.clanWebhookUrl(), message);
                    }

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

    @Schedule(period = 30, unit = ChronoUnit.SECONDS, asynchronous = true)
    public void checkEventAlarms()
    {
        if (!isAccessKeyValid() || !config.enableNotifications() || activeEvents.isEmpty()) {
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
}