package net.runelite.client.plugins.flippingcopilot.ui;

import static net.runelite.client.plugins.flippingcopilot.ui.UIUtilities.getProfitColor;

import net.runelite.client.plugins.flippingcopilot.controller.FlipTracker;
import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotConfig;
import net.runelite.client.plugins.flippingcopilot.model.Flip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;


public class StatsPanel extends JPanel {
    private final JLabel profitText = new JLabel();
    private final JPanel flipLogPanel = new JPanel();
    private final FlippingCopilotConfig config;

    public StatsPanel(FlippingCopilotConfig config) {
        this.config = config;
        setLayout(new BorderLayout(3, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setupTotalProfit();
        setupFlipLog();
    }

    void setupTotalProfit() {
        JLabel profitTitle = new JLabel("<html><b>Session Profit:<b></html>");
        profitTitle.setHorizontalAlignment(SwingConstants.CENTER);
        profitTitle.setForeground(Color.WHITE);

        profitText.setHorizontalAlignment(SwingConstants.CENTER);
        profitText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        profitText.setFont(FontManager.getRunescapeBoldFont());

        add(profitTitle, BorderLayout.NORTH);
        add(profitText, BorderLayout.CENTER);
        updateProfitText(0);
    }

    public void updateFlips(FlipTracker flipTracker, Client client) {
        updateProfitText(flipTracker.getProfit());
        refreshFlipLog(flipTracker.getFlips(), client);
    }

    private void updateProfitText(long profit) {
        profitText.setText(((profit >= 0) ? "" : "-")
                + UIUtilities.quantityToRSDecimalStack(Math.abs(profit), true) + " gp");
        profitText.setToolTipText("Total Profit: " + QuantityFormatter.formatNumber(profit) + " gp");
        profitText.setForeground(getProfitColor(profit, config));
    }

    private void setupFlipLog() {
        flipLogPanel.setLayout(new BoxLayout(flipLogPanel, BoxLayout.Y_AXIS));
        flipLogPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(flipLogPanel, BorderLayout.SOUTH);

    }

    private void refreshFlipLog(Collection<Flip> flips, Client client) {
        flipLogPanel.removeAll();
        for (Flip flip : flips) {
            String itemName = client.getItemDefinition(flip.getItemId()).getName();
            FlipPanel flipPanel = new FlipPanel(itemName, flip.getProfit(), config);
            flipLogPanel.add(flipPanel);
        }
        flipLogPanel.revalidate();
        flipLogPanel.repaint();
    }


}
