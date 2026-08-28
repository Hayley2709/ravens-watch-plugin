package com.ravenswatch;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class RavensWatchPanel extends PluginPanel {

    private final JButton calendarHeaderBtn;
    private final JPanel calendarContentPanel;
    private final JPanel motmContainer;
    private final JLabel motmNameLabel;
    private final JLabel motmReasonLabel;
    private final JLabel memberCountLabel;
    private List<String[]> currentEvents;

    public RavensWatchPanel() {
        super();

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // --- Title ---
        JLabel mainTitle = new JLabel("<html><div style='text-align: center;'><strong>Raven's Watch Clan<br>Official Plugin</strong></div></html>");
        mainTitle.setFont(FontManager.getRunescapeBoldFont());
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        mainTitle.setBorder(new EmptyBorder(5, 0, 5, 0));

        // --- Member Count (Wise Old Man) ---
        memberCountLabel = new JLabel("Total Members: Loading...");
        memberCountLabel.setFont(FontManager.getRunescapeFont());
        memberCountLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
        memberCountLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        memberCountLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // --- MOTM Card ---
        motmContainer = new JPanel();
        motmContainer.setLayout(new BoxLayout(motmContainer, BoxLayout.Y_AXIS));
        motmContainer.setBorder(new EmptyBorder(8, 8, 8, 8));
        motmContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        motmContainer.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        motmContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        motmContainer.setVisible(false);

        JLabel motmHeader = new JLabel("⭐ Golden Raven ⭐");
        motmHeader.setFont(FontManager.getRunescapeBoldFont());
        motmHeader.setForeground(Color.decode("#FFD700"));
        motmHeader.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        motmNameLabel = new JLabel();
        motmNameLabel.setForeground(Color.WHITE);
        motmNameLabel.setFont(FontManager.getRunescapeBoldFont());
        motmNameLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        motmNameLabel.setBorder(new EmptyBorder(2, 0, 4, 0));

        motmReasonLabel = new JLabel();
        motmReasonLabel.setForeground(Color.LIGHT_GRAY);
        motmReasonLabel.setFont(FontManager.getRunescapeFont());
        motmReasonLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        motmContainer.add(motmHeader);
        motmContainer.add(motmNameLabel);
        motmContainer.add(motmReasonLabel);

        // --- Calendar Section ---
        calendarHeaderBtn = new JButton("▼ Raven's Watch Event Calendar");
        calendarHeaderBtn.setFont(FontManager.getRunescapeBoldFont());
        calendarHeaderBtn.setForeground(Color.WHITE);
        calendarHeaderBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
        calendarHeaderBtn.setFocusPainted(false);
        calendarHeaderBtn.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        calendarHeaderBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

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

        // Assemble Layout
        this.add(mainTitle);
        this.add(memberCountLabel);
        this.add(motmContainer);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(calendarHeaderBtn);
        this.add(calendarContentPanel);
    }

    public void setMemberCount(String text) {
        memberCountLabel.setText(text);
        revalidate();
        repaint();
    }

    public void updateMotmDisplay(String name, String reason) {
        if (name == null || name.isEmpty()) {
            motmContainer.setVisible(false);
        } else {
            motmNameLabel.setText(name);

            String displayReason = (reason != null && !reason.isEmpty()) ? reason : "Outstanding Contribution";
            motmReasonLabel.setText("<html><body style='width: 150px; word-wrap: break-word;'>" + displayReason + "</body></html>");

            motmContainer.setVisible(true);
        }
        revalidate();
        repaint();
    }

    public void updateEventsList(List<String[]> formattedEvents) {
        this.currentEvents = formattedEvents;
        calendarContentPanel.removeAll();

        JLabel tzLabel = new JLabel("<html><font color='gray'>Times adjusted for your local timezone</font></html>");
        tzLabel.setFont(FontManager.getRunescapeFont());
        calendarContentPanel.add(tzLabel);

        if (formattedEvents == null || formattedEvents.isEmpty()) {
            JLabel noEventsLabel = new JLabel("No upcoming events found.");
            noEventsLabel.setForeground(Color.GRAY);
            calendarContentPanel.add(noEventsLabel);
        } else {
            for (String[] eventData : formattedEvents) {
                JPanel eventContainer = new JPanel(new GridLayout(2, 1, 0, 2));
                eventContainer.setBorder(new EmptyBorder(6, 6, 6, 6));
                eventContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

                JLabel titleLabel = new JLabel(eventData[0]);
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setFont(FontManager.getRunescapeBoldFont());

                JLabel dateLabel = new JLabel(eventData[1]);
                dateLabel.setForeground(Color.GRAY);
                dateLabel.setFont(FontManager.getRunescapeFont());

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