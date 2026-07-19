package com.googlecalendar;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import net.runelite.client.ui.PluginPanel;

public class GoogleCalendarPanel extends PluginPanel {

    public GoogleCalendarPanel() {
        super();
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new GridLayout(0, 1, 0, 10));

        // Header text with HTML wrapping for startup
        JLabel title = new JLabel("<html><strong>Raven's Watch Event Calendar</strong><br><font color='gray'>Adjusted for your timezone</font></html>");
        title.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
        title.setForeground(Color.WHITE);
        add(title);
    }

    public void updateEventsList(List<String[]> formattedEvents) {
        this.removeAll();

        // Header text with HTML wrapping when data updates
        JLabel title = new JLabel("<html><strong>Raven's Watch Event Calendar</strong><br><font color='gray'>Adjusted for your timezone</font></html>");
        title.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
        title.setForeground(Color.WHITE);
        this.add(title);

        if (formattedEvents == null || formattedEvents.isEmpty()) {
            JLabel noEventsLabel = new JLabel("No upcoming events found.");
            noEventsLabel.setForeground(Color.GRAY);
            this.add(noEventsLabel);
        } else {
            for (String[] eventData : formattedEvents) {
                String eventTitle = eventData[0];
                String eventDate = eventData[1];

                JPanel eventContainer = new JPanel(new GridLayout(2, 1, 0, 2));
                eventContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
                eventContainer.setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR);

                JLabel titleLabel = new JLabel(eventTitle);
                titleLabel.setForeground(Color.WHITE);

                JLabel dateLabel = new JLabel(eventDate);
                dateLabel.setForeground(Color.GRAY);

                eventContainer.add(titleLabel);
                eventContainer.add(dateLabel);

                this.add(eventContainer);
            }
        }

        this.revalidate();
        this.repaint();
    }
}
