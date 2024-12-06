package net.runelite.client.plugins.flippingcopilot.ui;

import static net.runelite.client.plugins.flippingcopilot.ui.UIUtilities.BUTTON_HOVER_LUMINANCE;

import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotPlugin;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import net.runelite.client.util.ImageUtil;

class PauseButton extends JButton
{
    private final FlippingCopilotPlugin plugin;

    private static final ImageIcon PLAY_ICON;
    private static final ImageIcon PAUSE_ICON;
    private static final ImageIcon PLAY_ICON_HOVER;
    private static final ImageIcon PAUSE_ICON_HOVER;

    static {
        var play = ImageUtil.loadImageResource(PauseButton.class,
            "/net/runelite/client/plugins/flippingcopilot/play.png");
        var pause = ImageUtil.loadImageResource(PauseButton.class,
            "/net/runelite/client/plugins/flippingcopilot/pause.png");
        PLAY_ICON = new ImageIcon(play);
        PAUSE_ICON = new ImageIcon(pause);
        PLAY_ICON_HOVER =  new ImageIcon(ImageUtil.luminanceScale(play, BUTTON_HOVER_LUMINANCE));
        PAUSE_ICON_HOVER = new ImageIcon(ImageUtil.luminanceScale(pause, BUTTON_HOVER_LUMINANCE));
    }

    public PauseButton(FlippingCopilotPlugin plugin) {
        super(PAUSE_ICON);
        this.plugin = plugin;
        setToolTipText("Pause suggestions");
        addActionListener(e -> {
            plugin.suggestionHandler.togglePause();
            update();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setIcon(plugin.suggestionHandler.isPaused() ? PLAY_ICON_HOVER : PAUSE_ICON_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setIcon(plugin.suggestionHandler.isPaused() ? PLAY_ICON : PAUSE_ICON);
            }
        });

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    private void update()
    {
        setIcon(plugin.suggestionHandler.isPaused() ? PLAY_ICON : PAUSE_ICON);
        setToolTipText(plugin.suggestionHandler.isPaused() ? "Unpause suggestions" :  "Pause suggestions");
    }
}
