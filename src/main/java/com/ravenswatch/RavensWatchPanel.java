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
    private final JPanel motmContainer;
    private final JLabel motmNameLabel;
    private final JLabel motmReasonLabel;
    private List<String[]> currentEvents;

    public RavensWatchPanel() {
        super();
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel mainTitle = new JLabel("<html><div style='text-align: center;'><strong>Raven's Watch Clan<br>Official Plugin</strong></div></html>");
        mainTitle.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        mainTitle.setBorder(new EmptyBorder(5, 0, 15, 0));

// --- MOTM Card ---
        motmContainer = new JPanel(new GridLayout(3, 1, 0, 2));
        motmContainer.setBorder(new EmptyBorder(8, 8, 8, 8));
        motmContainer.setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR);
        motmContainer.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        motmContainer.setVisible(false); // Hidden until JSON data loads successfully

        JLabel motmHeader = new JLabel("⭐ Golden Raven");
        motmHeader.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        motmHeader.setForeground(Color.YELLOW);

        motmNameLabel = new JLabel();
        motmNameLabel.setForeground(Color.WHITE);
        motmNameLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());

        motmReasonLabel = new JLabel();
        motmReasonLabel.setForeground(Color.GRAY);
        motmReasonLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());

        motmContainer.add(motmHeader);
        motmContainer.add(motmNameLabel);
        motmContainer.add(motmReasonLabel);

// --- Calendar Section ---
        calendarHeaderBtn = new JButton("▼ Raven's Watch Event Calendar");
        calendarHeaderBtn.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        calendarHeaderBtn.setForeground(Color.WHITE);
        calendarHeaderBtn.setBackground(net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR);
        calendarHeaderBtn.setFocusPainted(false);
        calendarHeaderBtn.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        calendarHeaderBtn.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30));

        calendarContentPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        calendarContentPanel.setBorder(new EmptyBorder(10, 5, 10, 5));
        calendarContentPanel.setVisible(true);

        calendarHeaderBtn.addActionListener(e -> {
            boolean isVisible = calendarContentPanel.isVisible();
            calendarContentPanel.setVisible(!isVisible);
            calendarHeaderBtn.setText(isVisible ? "▶ Raven's Watch Event Calendar" : "▼ Raven's Watch Event Calendar");
            revalidate();
            repaint();
        });

        this.add(mainTitle);
        this.add(motmContainer);
        this.add(calendarHeaderBtn);
        this.add(calendarContentPanel);
    }

    public void updateMotmDisplay(String name, String reason) {
        if (name == null || name.isEmpty()) {
            motmContainer.setVisible(false);
        } else {
            motmNameLabel.setText(name);
            motmReasonLabel.setText(reason != null ? reason : "Outstanding Contribution");
            motmContainer.setVisible(true);
        }
        revalidate();
        repaint();
    }

    public void updateEventsList(List<String[]> formattedEvents) {
        this.currentEvents = formattedEvents;
        calendarContentPanel.removeAll();

        JLabel tzLabel = new JLabel("<html><font color='gray'>Times adjusted for your local timezone</font></html>");
        tzLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
        calendarContentPanel.add(tzLabel);

        if (formattedEvents == null || formattedEvents.isEmpty()) {
            JLabel noEventsLabel = new JLabel("No upcoming events found.");
            noEventsLabel.setForeground(Color.GRAY);
            calendarContentPanel.add(noEventsLabel);
        } else {
            for (String[] eventData : formattedEvents) {
                JPanel eventContainer = new JPanel(new GridLayout(2, 1, 0, 2));
                eventContainer.setBorder(new EmptyBorder(6, 6, 6, 6));
                eventContainer.setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR);

                JLabel titleLabel = new JLabel(eventData[0]);
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());

                JLabel dateLabel = new JLabel(eventData[1]);
                dateLabel.setForeground(Color.GRAY);
                dateLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());

                eventContainer.add(titleLabel);
                eventContainer.add(dateLabel);
                calendarContentPanel.add(eventContainer);
            }
        }

        calendarContentPanel.revalidate();
        calendarContentPanel.repaint();
        revalidate();
        repaint();
    }
}
