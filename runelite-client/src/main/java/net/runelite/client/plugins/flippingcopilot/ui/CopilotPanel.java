package net.runelite.client.plugins.flippingcopilot.ui;

import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotConfig;
import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotPlugin;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;


public class CopilotPanel extends JPanel {
    public final SuggestionPanel suggestionPanel;
    private final ControlPanel controlPanel;
    public final StatsPanel statsPanel;


public CopilotPanel(FlippingCopilotConfig config) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    suggestionPanel = new SuggestionPanel(config);
    controlPanel = new ControlPanel();
    statsPanel = new StatsPanel(config);
    add(suggestionPanel);
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(controlPanel);
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(statsPanel);
}
    public void init(FlippingCopilotPlugin plugin) {
        controlPanel.init(plugin);
        suggestionPanel.init(plugin);
    }
}
