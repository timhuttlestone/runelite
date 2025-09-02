package net.runelite.client.plugins.flippingcopilot.ui;


import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class PreferencesToggleButton extends JToggleButton
{
    private static final ImageIcon ON_SWITCHER;
    private static final ImageIcon OFF_SWITCHER;
    private final String unSelectedToolTipText;
    private final String selectedToolTipText;


    public PreferencesToggleButton(String selectedToolTipText, String unSelectedToolTipText) {
        super(OFF_SWITCHER);
        setSelectedIcon(ON_SWITCHER);
        SwingUtil.removeButtonDecorations(this);
        setPreferredSize(new Dimension(25, 25));
        addItemListener(l -> updateTooltip());
        updateTooltip();
        this.selectedToolTipText = selectedToolTipText;
        this.unSelectedToolTipText = unSelectedToolTipText;
    }

    static
    {
        BufferedImage onSwitcher = ImageUtil.loadImageResource(CopilotPanel.class, "/net/runelite/client/plugins/flippingcopilot/switcher_on.png");
        ON_SWITCHER = new ImageIcon(onSwitcher);
        OFF_SWITCHER = new ImageIcon(ImageUtil.flipImage(
                ImageUtil.luminanceScale(
                        ImageUtil.grayscaleImage(onSwitcher),
                        0.61f
                ),
                true,
                false
        ));
    }

    private void updateTooltip()
    {
        setToolTipText(isSelected() ? selectedToolTipText :  unSelectedToolTipText);
    }
}
