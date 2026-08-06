package com.ravenswatch;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import net.runelite.client.ui.PluginPanel;

public class RavensWatchPanel extends PluginPanel {

    private final JButton calendarHeaderBtn;
    private final JPanel calendarContentPanel;
    private List<String[]> currentEvents;

    public RavensWatchPanel() {
        super();
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // 1. Create the Master Title at the top of the plugin
        JLabel mainTitle = new JLabel("<html><div style='text-align: center;'><strong>Raven's Watch Clan<br>Official Plugin</strong></div></html>");
        mainTitle.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        // Add 15px of space underneath the main title so the dropdown doesn't hug it too tightly
        mainTitle.setBorder(new EmptyBorder(5, 0, 15, 0));

        // 2. Create the Collapsible Header Button
        calendarHeaderBtn = new JButton("▼ Raven's Watch Event Calendar");
        calendarHeaderBtn.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        calendarHeaderBtn.setForeground(Color.WHITE);
        calendarHeaderBtn.setBackground(net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR);
        calendarHeaderBtn.setFocusPainted(false);
        calendarHeaderBtn.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        calendarHeaderBtn.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30));

        // 3. Create the Container that holds the calendar contents
        calendarContentPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        calendarContentPanel.setBorder(new EmptyBorder(10, 5, 10, 5));
        calendarContentPanel.setVisible(true); // Open by default on startup

        // 4. Add the Toggle Action
        calendarHeaderBtn.addActionListener(e -> {
            boolean isVisible = calendarContentPanel.isVisible();
            calendarContentPanel.setVisible(!isVisible);

            if (isVisible) {
                calendarHeaderBtn.setText("▶ Raven's Watch Event Calendar");
            } else {
                calendarHeaderBtn.setText("▼ Raven's Watch Event Calendar");
            }

            revalidate();
            repaint();
        });

        // Assemble the layout (Main title goes first!)
        this.add(mainTitle);
        this.add(calendarHeaderBtn);
        this.add(calendarContentPanel);
    }

    public void updateEventsList(List<String[]> formattedEvents) {
        this.currentEvents = formattedEvents;

        // Clear out old cards inside the content panel only
        calendarContentPanel.removeAll();

        // Timezone helper label at the top of the expanded section
        JLabel tzLabel = new JLabel("<html><font color='gray'>Times adjusted for your local timezone</font></html>");
        tzLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
        calendarContentPanel.add(tzLabel);

        if (formattedEvents == null || formattedEvents.isEmpty()) {
            JLabel noEventsLabel = new JLabel("No upcoming events found.");
            noEventsLabel.setForeground(Color.GRAY);
            calendarContentPanel.add(noEventsLabel);
        } else {
            for (String[] eventData : formattedEvents) {
                String eventTitle = eventData[0];
                String eventDate = eventData[1];

                JPanel eventContainer = new JPanel(new GridLayout(2, 1, 0, 2));
                eventContainer.setBorder(new EmptyBorder(6, 6, 6, 6));
                eventContainer.setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR);

                JLabel titleLabel = new JLabel(eventTitle);
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());

                JLabel dateLabel = new JLabel(eventDate);
                dateLabel.setForeground(Color.GRAY);
                dateLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());

                eventContainer.add(titleLabel);
                eventContainer.add(dateLabel);

                calendarContentPanel.add(eventContainer);
            }
        }

        // Refresh layouts smoothly
        calendarContentPanel.revalidate();
        calendarContentPanel.repaint();
        this.revalidate();
        this.repaint();
    }
}
