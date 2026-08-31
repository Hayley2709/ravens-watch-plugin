package com.ravenswatch;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
import okhttp3.OkHttpClient;

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

    @Inject private ClientToolbar clientToolbar;
    @Inject private RavensWatchConfig config;
    @Inject private RavensWatchClient calendarClient;
    @Inject private Notifier notifier;
    @Inject private Client client;
    @Inject private OkHttpClient okHttpClient;

    private RavensWatchPanel panel;
    private NavigationButton navButton;

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
                .tooltip("Raven's Watch")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        // Fetch all data sections on plugin startup
        fetchAllData();
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("ravenswatch"))
        {
            return;
        }

        if (event.getKey().equals("apiKey") || event.getKey().equals("calendarId") || event.getKey().equals("upcomingMonthOnly"))
        {
            fetchCalendarEvents();
        }
    }

    @Schedule(period = 10, unit = ChronoUnit.MINUTES)
    public void fetchAllData()
    {
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
                if (response == null || response.items == null)
                {
                    panel.updateEventsList(new ArrayList<>());
                    return;
                }

                DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_DATE_TIME;
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm");
                ZoneId localZone = ZoneId.systemDefault();
                ZonedDateTime now = ZonedDateTime.now(localZone);
                ZonedDateTime oneMonthFromNow = now.plusMonths(1);

                List<String[]> formattedEvents = new ArrayList<>();

                for (RavensWatchClient.CalendarEvent event : response.items)
                {
                    if (event.start != null && event.start.dateTime != null)
                    {
                        try
                        {
                            ZonedDateTime eventTime = ZonedDateTime.parse(event.start.dateTime, inputFormatter).withZoneSameInstant(localZone);

                            if (config.upcomingMonthOnly() && eventTime.isAfter(oneMonthFromNow))
                            {
                                continue;
                            }

                            formattedEvents.add(new String[]{
                                    event.summary != null ? event.summary : "Clan Event",
                                    eventTime.format(outputFormatter)
                            });
                        }
                        catch (Exception e)
                        {
                            log.error("Error parsing event date: {}", event.start.dateTime, e);
                        }
                    }
                }

                panel.updateEventsList(formattedEvents);
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
}