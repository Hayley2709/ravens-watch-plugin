package com.ravenswatch;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class RavensWatchPanel extends PluginPanel {

    private final JButton calendarHeaderBtn;
    private final JPanel calendarContentPanel;

    // Recent Drops components
    private final JButton dropsHeaderBtn;
    private final JPanel dropsContentPanel;
    private final List<String> recentDrops = new ArrayList<>();

    private final JPanel motmContainer;
    private final JLabel motmNameLabel;
    private final JLabel motmReasonLabel;
    private final JLabel memberCountLabel;
    private List<String[]> currentEvents;

    private RavensWatchPlugin plugin;

    public RavensWatchPanel(RavensWatchPlugin plugin) {
        super();
        this.plugin = plugin;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // --- Title ---
        JLabel mainTitle = new JLabel("<html><body style='width: 170px; text-align: center; word-wrap: break-word;'><strong>Raven's Watch Clan<br>Official Plugin</strong></body></html>", SwingConstants.CENTER);
        mainTitle.setFont(FontManager.getRunescapeBoldFont());
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        mainTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        mainTitle.setBorder(new EmptyBorder(5, 0, 5, 0));

        // --- Member Count (Wise Old Man) ---
        memberCountLabel = new JLabel("Total Members: Loading...", SwingConstants.CENTER);
        memberCountLabel.setFont(FontManager.getRunescapeFont());
        memberCountLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
        memberCountLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        memberCountLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        memberCountLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // --- MOTM Card ---
        motmContainer = new JPanel();
        motmContainer.setLayout(new BoxLayout(motmContainer, BoxLayout.Y_AXIS));
        motmContainer.setBorder(new EmptyBorder(8, 8, 8, 8));
        motmContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        motmContainer.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        motmContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
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
        calendarHeaderBtn = new JButton("<html><body style='width: 155px;'>▼ Raven's Watch Event Calendar</body></html>");
        calendarHeaderBtn.setFont(FontManager.getRunescapeBoldFont());
        calendarHeaderBtn.setForeground(Color.WHITE);
        calendarHeaderBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
        calendarHeaderBtn.setFocusPainted(false);
        calendarHeaderBtn.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        calendarHeaderBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        calendarContentPanel = new JPanel();
        calendarContentPanel.setLayout(new BoxLayout(calendarContentPanel, BoxLayout.Y_AXIS));
        calendarContentPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        calendarContentPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        calendarContentPanel.setVisible(true);

        calendarHeaderBtn.addActionListener(e -> {
            boolean isVisible = calendarContentPanel.isVisible();
            calendarContentPanel.setVisible(!isVisible);
            calendarHeaderBtn.setText(isVisible ?
                    "<html><body style='width: 155px;'>▶ Raven's Watch Event Calendar</body></html>" :
                    "<html><body style='width: 155px;'>▼ Raven's Watch Event Calendar</body></html>");
            revalidate();
            repaint();
        });

        // --- Recent Drops Section ---
        dropsHeaderBtn = new JButton("<html><body style='width: 155px;'>▼ Recent Clan Broadcasts</body></html>");
        dropsHeaderBtn.setFont(FontManager.getRunescapeBoldFont());
        dropsHeaderBtn.setForeground(Color.WHITE);
        dropsHeaderBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
        dropsHeaderBtn.setFocusPainted(false);
        dropsHeaderBtn.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        dropsHeaderBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        dropsContentPanel = new JPanel();
        dropsContentPanel.setLayout(new BoxLayout(dropsContentPanel, BoxLayout.Y_AXIS));
        dropsContentPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        dropsContentPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        dropsContentPanel.setVisible(true);

        dropsHeaderBtn.addActionListener(e -> {
            boolean isVisible = dropsContentPanel.isVisible();
            dropsContentPanel.setVisible(!isVisible);
            dropsHeaderBtn.setText(isVisible ?
                    "<html><body style='width: 155px;'>▶ Recent Clan Broadcasts</body></html>" :
                    "<html><body style='width: 155px;'>▼ Recent Clan Broadcasts</body></html>");
            revalidate();
            repaint();
        });

        refreshDropsUI();

        // Assemble Layout
        this.add(mainTitle);
        this.add(Box.createRigidArea(new Dimension(0, 8)));
        this.add(memberCountLabel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(motmContainer);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(calendarHeaderBtn);
        this.add(calendarContentPanel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(dropsHeaderBtn);
        this.add(dropsContentPanel);
    }

    public void setMemberCount(String text) {
        SwingUtilities.invokeLater(() -> {
            memberCountLabel.setText(text);
            revalidate();
            repaint();
        });
    }

    public void addRecentDrop(String dropText) {
        SwingUtilities.invokeLater(() -> {
            recentDrops.add(0, dropText);
            if (recentDrops.size() > 20) {
                recentDrops.remove(recentDrops.size() - 1);
            }
            refreshDropsUI();
        });
    }

    private void refreshDropsUI() {
        dropsContentPanel.removeAll();

        if (recentDrops.isEmpty()) {
            JLabel noDropsLabel = new JLabel("No recent drops logged yet.");
            noDropsLabel.setForeground(Color.GRAY);
            noDropsLabel.setFont(FontManager.getRunescapeFont());
            noDropsLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            dropsContentPanel.add(noDropsLabel);
        } else {
            for (int i = 0; i < recentDrops.size(); i++) {
                String drop = recentDrops.get(i);

                JPanel dropCard = new JPanel();
                dropCard.setLayout(new BoxLayout(dropCard, BoxLayout.Y_AXIS));
                dropCard.setBorder(new EmptyBorder(6, 8, 6, 8));
                dropCard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                dropCard.setAlignmentX(JPanel.LEFT_ALIGNMENT);
                dropCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

                JLabel dropLabel = new JLabel("<html><body style='width: 170px; word-wrap: break-word;'>" + drop + "</body></html>");
                dropLabel.setForeground(Color.WHITE);
                dropLabel.setFont(FontManager.getRunescapeFont());
                dropLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

                dropCard.add(dropLabel);
                dropsContentPanel.add(dropCard);

                if (i < recentDrops.size() - 1) {
                    dropsContentPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
        }

        dropsContentPanel.revalidate();
        dropsContentPanel.repaint();
        revalidate();
        repaint();
    }

    public void updateMotmDisplay(String name, String reason) {
        SwingUtilities.invokeLater(() -> {
            if (name == null || name.isEmpty()) {
                motmContainer.setVisible(false);
            } else {
                motmNameLabel.setText("<html><body style='width: 170px; word-wrap: break-word;'>" + name + "</body></html>");

                String displayReason = (reason != null && !reason.isEmpty()) ? reason : "Outstanding Contribution";
                motmReasonLabel.setText("<html><body style='width: 170px; word-wrap: break-word;'>" + displayReason + "</body></html>");

                motmContainer.setVisible(true);
            }

            motmContainer.revalidate();
            motmContainer.repaint();

            if (getParent() != null) {
                getParent().revalidate();
                getParent().repaint();
            }

            revalidate();
            repaint();
        });
    }

    public void updateEventsList(List<String[]> formattedEvents) {
        SwingUtilities.invokeLater(() -> {
            this.currentEvents = formattedEvents;
            calendarContentPanel.removeAll();

            JLabel tzLabel = new JLabel("<html><font color='gray'>Times adjusted for your local timezone</font></html>");
            tzLabel.setFont(FontManager.getRunescapeFont());
            tzLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            calendarContentPanel.add(tzLabel);
            calendarContentPanel.add(Box.createRigidArea(new Dimension(0, 6)));

            if (formattedEvents == null || formattedEvents.isEmpty()) {
                JLabel noEventsLabel = new JLabel("No upcoming events found.");
                noEventsLabel.setForeground(Color.GRAY);
                noEventsLabel.setFont(FontManager.getRunescapeFont());
                noEventsLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                calendarContentPanel.add(noEventsLabel);
            } else {
                for (int i = 0; i < formattedEvents.size(); i++) {
                    String[] eventData = formattedEvents.get(i);

                    JPanel eventContainer = new JPanel();
                    eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.Y_AXIS));
                    eventContainer.setBorder(new EmptyBorder(6, 8, 6, 8));
                    eventContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    eventContainer.setAlignmentX(JPanel.LEFT_ALIGNMENT);
                    eventContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

                    JLabel titleLabel = new JLabel("<html><body style='width: 170px; word-wrap: break-word;'>" + eventData[0] + "</body></html>");
                    titleLabel.setForeground(Color.WHITE);
                    titleLabel.setFont(FontManager.getRunescapeBoldFont());
                    titleLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

                    JLabel dateLabel = new JLabel("<html><body style='width: 170px; word-wrap: break-word;'><font color='gray'>" + eventData[1] + "</font></body></html>");
                    dateLabel.setFont(FontManager.getRunescapeFont());
                    dateLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                    dateLabel.setBorder(new EmptyBorder(2, 0, 0, 0));

                    eventContainer.add(titleLabel);
                    eventContainer.add(dateLabel);
                    calendarContentPanel.add(eventContainer);

                    if (i < formattedEvents.size() - 1) {
                        calendarContentPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                    }
                }
            }

            calendarContentPanel.revalidate();
            calendarContentPanel.repaint();
            revalidate();
            repaint();
        });
    }
}