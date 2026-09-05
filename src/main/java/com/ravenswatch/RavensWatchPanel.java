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
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

public class RavensWatchPanel extends PluginPanel {

    private final JButton calendarHeaderBtn;
    private final JPanel calendarContentPanel;
    private final CardLayout calendarCardLayout = new CardLayout();
    private final JPanel calendarCardContainer = new JPanel(calendarCardLayout);

    private final JButton compsHeaderBtn;
    private final JPanel compsContentPanel;
    private final CardLayout compsCardLayout = new CardLayout();
    private final JPanel compsCardContainer = new JPanel(compsCardLayout);

    private final JButton dropsHeaderBtn;
    private final JPanel dropsContentPanel;
    private final CardLayout dropsCardLayout = new CardLayout();
    private final JPanel dropsCardContainer = new JPanel(dropsCardLayout);

    private final List<String> recentDrops = new ArrayList<>();
    private final JPanel motmContainer;
    private final JLabel memberCountLabel;

    private static final String UNLOCKED_CARD = "UNLOCKED";
    private static final String LOCKED_CARD = "LOCKED";

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

        // --- Member Count ---
        memberCountLabel = new JLabel("Total Members: Protected", SwingConstants.CENTER);
        memberCountLabel.setFont(FontManager.getRunescapeFont());
        memberCountLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
        memberCountLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        memberCountLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        memberCountLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // --- Golden Ravens Container ---
        motmContainer = new JPanel();
        motmContainer.setLayout(new BoxLayout(motmContainer, BoxLayout.Y_AXIS));
        motmContainer.setBorder(new EmptyBorder(8, 8, 8, 8));
        motmContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        motmContainer.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        motmContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        motmContainer.setVisible(false);

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

        JPanel calendarLocked = createLockedPlaceholder("Enter clan access key in plugin settings to view events.");
        calendarCardContainer.setLayout(calendarCardLayout);
        calendarCardContainer.add(calendarContentPanel, UNLOCKED_CARD);
        calendarCardContainer.add(calendarLocked, LOCKED_CARD);
        calendarCardContainer.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        calendarHeaderBtn.addActionListener(e -> {
            boolean isVisible = calendarCardContainer.isVisible();
            calendarCardContainer.setVisible(!isVisible);
            calendarHeaderBtn.setText(isVisible ?
                    "<html><body style='width: 155px;'>▶ Raven's Watch Event Calendar</body></html>" :
                    "<html><body style='width: 155px;'>▼ Raven's Watch Event Calendar</body></html>");
            revalidate();
            repaint();
        });

        // --- Competitions Section ---
        compsHeaderBtn = new JButton("<html><body style='width: 155px;'>▼ Clan Competitions</body></html>");
        compsHeaderBtn.setFont(FontManager.getRunescapeBoldFont());
        compsHeaderBtn.setForeground(Color.WHITE);
        compsHeaderBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
        compsHeaderBtn.setFocusPainted(false);
        compsHeaderBtn.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        compsHeaderBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        compsContentPanel = new JPanel();
        compsContentPanel.setLayout(new BoxLayout(compsContentPanel, BoxLayout.Y_AXIS));
        compsContentPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        compsContentPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        JPanel compsLocked = createLockedPlaceholder("Enter clan access key in plugin settings to view competitions.");
        compsCardContainer.setLayout(compsCardLayout);
        compsCardContainer.add(compsContentPanel, UNLOCKED_CARD);
        compsCardContainer.add(compsLocked, LOCKED_CARD);
        compsCardContainer.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        compsHeaderBtn.addActionListener(e -> {
            boolean isVisible = compsCardContainer.isVisible();
            compsCardContainer.setVisible(!isVisible);
            compsHeaderBtn.setText(isVisible ?
                    "<html><body style='width: 155px;'>▶ Clan Competitions</body></html>" :
                    "<html><body style='width: 155px;'>▼ Clan Competitions</body></html>");
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

        JPanel dropsLocked = createLockedPlaceholder("Enter clan access key in plugin settings to view broadcasts.");
        dropsCardContainer.setLayout(dropsCardLayout);
        dropsCardContainer.add(dropsContentPanel, UNLOCKED_CARD);
        dropsCardContainer.add(dropsLocked, LOCKED_CARD);
        dropsCardContainer.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        dropsHeaderBtn.addActionListener(e -> {
            boolean isVisible = dropsCardContainer.isVisible();
            dropsCardContainer.setVisible(!isVisible);
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
        this.add(calendarCardContainer);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(compsHeaderBtn);
        this.add(compsCardContainer);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(dropsHeaderBtn);
        this.add(dropsCardContainer);
    }

    public void setPanelUnlocked(boolean unlocked) {
        SwingUtilities.invokeLater(() -> {
            String card = unlocked ? UNLOCKED_CARD : LOCKED_CARD;
            calendarCardLayout.show(calendarCardContainer, card);
            compsCardLayout.show(compsCardContainer, card);
            dropsCardLayout.show(dropsCardContainer, card);

            if (!unlocked) {
                motmContainer.setVisible(false);
                memberCountLabel.setText("Total Members: Protected");
            }
            revalidate();
            repaint();
        });
    }

    private JPanel createLockedPlaceholder(String text) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel lockLabel = new JLabel("<html><body style='width: 155px; text-align: center;'><font color='gray'>🔒 " + text + "</font></body></html>");
        lockLabel.setFont(FontManager.getRunescapeFont());
        lockLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        panel.add(lockLabel);
        return panel;
    }

    public void setMemberCount(String text) {
        SwingUtilities.invokeLater(() -> {
            memberCountLabel.setText(text);
            revalidate();
            repaint();
        });
    }

    public void updateCompetitionsList(List<RavensWatchClient.WomCompetition> competitions) {
        SwingUtilities.invokeLater(() -> {
            compsContentPanel.removeAll();

            if (competitions == null || competitions.isEmpty()) {
                JLabel noCompsLabel = new JLabel("No competitions found.");
                noCompsLabel.setForeground(Color.GRAY);
                noCompsLabel.setFont(FontManager.getRunescapeFont());
                noCompsLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                compsContentPanel.add(noCompsLabel);
            } else {
                List<RavensWatchClient.WomCompetition> activeCompetitions = competitions.stream()
                        .filter(RavensWatchClient.WomCompetition::isActive)
                        .collect(Collectors.toList());

                if (activeCompetitions.isEmpty()) {
                    JLabel noActiveCompsLabel = new JLabel("No active competitions found.");
                    noActiveCompsLabel.setForeground(Color.GRAY);
                    noActiveCompsLabel.setFont(FontManager.getRunescapeFont());
                    noActiveCompsLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                    compsContentPanel.add(noActiveCompsLabel);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
                    ZoneId localZone = ZoneId.systemDefault();

                    for (int i = 0; i < activeCompetitions.size(); i++) {
                        RavensWatchClient.WomCompetition comp = activeCompetitions.get(i);

                        JPanel compCard = new JPanel();
                        compCard.setLayout(new BoxLayout(compCard, BoxLayout.Y_AXIS));
                        compCard.setBorder(new EmptyBorder(6, 8, 6, 8));
                        compCard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                        compCard.setAlignmentX(JPanel.LEFT_ALIGNMENT);
                        compCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                        compCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                        compCard.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                LinkBrowser.browse("https://wiseoldman.net/competitions/" + comp.id);
                            }
                        });

                        JLabel titleLabel = new JLabel("<html><body style='width: 155px;'>🏆 " + comp.title + "</body></html>");
                        titleLabel.setForeground(Color.WHITE);
                        titleLabel.setFont(FontManager.getRunescapeBoldFont());
                        titleLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

                        String dateText = "Dates TBD";
                        if (comp.startsAt != null && comp.endsAt != null) {
                            ZonedDateTime start = ZonedDateTime.parse(comp.startsAt).withZoneSameInstant(localZone);
                            ZonedDateTime end = ZonedDateTime.parse(comp.endsAt).withZoneSameInstant(localZone);
                            dateText = start.format(formatter) + " - " + end.format(formatter);
                        }

                        JLabel dateLabel = new JLabel("<html><body style='width: 170px;'><font color='gray'>" + dateText + "</font></body></html>");
                        dateLabel.setFont(FontManager.getRunescapeFont());
                        dateLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

                        compCard.add(titleLabel);
                        compCard.add(dateLabel);

                        List<RavensWatchClient.WomParticipation> participations = comp.participations;
                        if (participations != null && !participations.isEmpty()) {
                            compCard.add(Box.createRigidArea(new Dimension(0, 4)));

                            List<RavensWatchClient.WomParticipation> top3 = participations.stream()
                                    .limit(3)
                                    .collect(Collectors.toList());

                            for (int rank = 0; rank < top3.size(); rank++) {
                                RavensWatchClient.WomParticipation p = top3.get(rank);
                                String medal = (rank == 0) ? "🥇 " : (rank == 1) ? "🥈 " : "🥉 ";
                                String username = p.player != null ? p.player.username : "Unknown";
                                int gained = p.progress != null ? (int) p.progress.gained : 0;

                                JLabel rankLabel = new JLabel("<html><body style='width: 170px;'><font color='#D3D3D3'>" + medal + username + " (+" + String.format("%,d", gained) + ")</font></body></html>");
                                rankLabel.setFont(FontManager.getRunescapeFont());
                                rankLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                                compCard.add(rankLabel);
                            }
                        }

                        compsContentPanel.add(compCard);

                        if (i < activeCompetitions.size() - 1) {
                            compsContentPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                        }
                    }
                }
            }

            compsContentPanel.revalidate();
            compsContentPanel.repaint();
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

    public void updateBroadcastsDisplay(List<String> broadcasts) {
        SwingUtilities.invokeLater(() -> {
            recentDrops.clear();
            if (broadcasts != null) {
                recentDrops.addAll(broadcasts);
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

    public void updateGoldenRavensDisplay(List<String> ravensList) {
        SwingUtilities.invokeLater(() -> {
            motmContainer.removeAll();

            JLabel motmHeader = new JLabel("⭐ Golden Ravens ⭐");
            motmHeader.setFont(FontManager.getRunescapeBoldFont());
            motmHeader.setForeground(Color.decode("#FFD700"));
            motmHeader.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            motmContainer.add(motmHeader);
            motmContainer.add(Box.createRigidArea(new Dimension(0, 4)));

            if (ravensList == null || ravensList.isEmpty()) {
                motmContainer.setVisible(false);
            } else {
                for (String raven : ravensList) {
                    JLabel ravenLabel = new JLabel("<html><body style='width: 170px; word-wrap: break-word;'>• " + raven + "</body></html>");
                    ravenLabel.setForeground(Color.WHITE);
                    ravenLabel.setFont(FontManager.getRunescapeFont());
                    ravenLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
                    ravenLabel.setBorder(new EmptyBorder(1, 0, 2, 0));
                    motmContainer.add(ravenLabel);
                }
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