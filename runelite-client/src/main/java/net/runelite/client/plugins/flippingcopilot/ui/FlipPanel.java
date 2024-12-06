package net.runelite.client.plugins.flippingcopilot.ui;

import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotConfig;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public class FlipPanel extends JPanel {

    public FlipPanel(String itemName, long profit, FlippingCopilotConfig config) {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JLabel itemNameLabel = new JLabel(UIUtilities.truncateString(itemName, 22));
        JLabel profitLabel = new JLabel(UIUtilities.formatProfit(profit));
        profitLabel.setForeground(UIUtilities.getProfitColor(profit, config));

        add(itemNameLabel, BorderLayout.LINE_START);
        add(profitLabel, BorderLayout.LINE_END);
    }
}