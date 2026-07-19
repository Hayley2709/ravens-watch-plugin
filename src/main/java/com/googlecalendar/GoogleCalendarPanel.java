package com.googlecalendar;

import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import java.awt.*;

public class GoogleCalendarPanel extends PluginPanel
{
    private final JPanel listContainer = new JPanel();

    public GoogleCalendarPanel()
    {
        super();
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Upcoming Events");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        add(new JScrollPane(listContainer), BorderLayout.CENTER);
    }

    public void updateEvents(GoogleCalendarClient.CalendarResponse response)
    {
        SwingUtilities.invokeLater(() -> {
            listContainer.removeAll();
            if (response.items == null || response.items.isEmpty()) {
                listContainer.add(new JLabel("No upcoming events found."));
            } else {
                for (GoogleCalendarClient.CalendarEvent event : response.items) {
                    JPanel itemPanel = new JPanel(new BorderLayout());
                    itemPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));

                    JLabel name = new JLabel(event.summary);
                    name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

                    String displayTime = event.start.dateTime != null ? event.start.dateTime : event.start.date;
                    JLabel time = new JLabel(displayTime);
                    time.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                    time.setForeground(Color.GRAY);

                    itemPanel.add(name, BorderLayout.NORTH);
                    itemPanel.add(time, BorderLayout.SOUTH);
                    itemPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                    listContainer.add(itemPanel);
                }
            }
            revalidate();
            repaint();
        });
    }
}
