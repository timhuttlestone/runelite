package net.runelite.client.plugins.flippingcopilot.ui;

import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotPlugin;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;

@Slf4j
public class ControlPanel extends JPanel {
    SellOnlyModeToggleButton sellOnlyModeToggleButton = new SellOnlyModeToggleButton();
    public ControlPanel() {
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new BorderLayout(3, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 50, 5, 50));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.setOpaque(false);
        add(buttonPanel, BorderLayout.CENTER);

        JLabel buttonText = new JLabel("Sell-only Mode");
        buttonPanel.add(buttonText, BorderLayout.LINE_START);
        buttonPanel.add(sellOnlyModeToggleButton, BorderLayout.LINE_END);
    }

    public void init(FlippingCopilotPlugin plugin) {
        sellOnlyModeToggleButton.addItemListener(i ->
        {
            plugin.accountStatus.setSellOnlyMode(sellOnlyModeToggleButton.isSelected());
            plugin.suggestionHandler.setSuggestionNeeded(true);
            log.debug("Sell only mode is now: {}", plugin.accountStatus.isSellOnlyMode());
        });
    }
}
