package net.runelite.client.plugins.flippingcopilot.ui;

import static net.runelite.client.plugins.flippingcopilot.ui.UIUtilities.buildButton;

import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotConfig;
import net.runelite.client.plugins.flippingcopilot.controller.FlippingCopilotPlugin;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

public class MainPanel extends PluginPanel {
    public LoginPanel loginPanel;
    public CopilotPanel copilotPanel;
    public Runnable onCopilotLogout;

    public MainPanel(FlippingCopilotConfig config) {
        setLayout(new BorderLayout());
        copilotPanel = new CopilotPanel(config);
    }

    public void init(FlippingCopilotPlugin plugin) {
        copilotPanel.init(plugin);
        this.loginPanel = plugin.copilotLoginController.getPanel();
        this.onCopilotLogout = plugin.copilotLoginController::onLogout;
        renderLoggedOutView();
    }

    public void renderLoggedOutView() {
        removeAll();
        add(constructTopBar(false), BorderLayout.NORTH);
        loginPanel.showLoginErrorMessage("");
        add(loginPanel);
        revalidate();
    }

    public void renderLoggedInView() {
        removeAll();
        add(constructTopBar(true), BorderLayout.NORTH);
        add(copilotPanel);
        revalidate();
    }

    private JPanel constructTopBar(boolean isLoggedIn) {
        JPanel container = new JPanel();
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setLayout(new BorderLayout());
        container.setBorder(new EmptyBorder(0, 0, 5, 0));
        JPanel topBar = new JPanel();
        topBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
        int columns = isLoggedIn ? 4 : 3;
        topBar.setLayout(new GridLayout(1, columns));

        JLabel github = buildTopBarUriButton(UIUtilities.githubIcon,
                "Flipping Copilot Github",
                "https://github.com/cbrewitt/flipping-copilot");
        topBar.add(github);

        JLabel discord = buildTopBarUriButton(UIUtilities.discordIcon,
                "Flipping Copilot Discord",
                "https://discord.gg/UyQxA4QJAq");
        topBar.add(discord);

        JLabel website = buildTopBarUriButton(UIUtilities.internetIcon,
                "Flipping Copilot website",
                "https://flippingcopilot.com");
        topBar.add(website);

        if (isLoggedIn) {
            BufferedImage icon = ImageUtil.loadImageResource(getClass(), UIUtilities.logoutIcon);
            JLabel logout = buildButton(icon, "Log out", () -> {
                onCopilotLogout.run();
                renderLoggedOutView();
            });
            topBar.add(logout);
        }

        container.add(topBar);
        container.setBorder(new EmptyBorder(3, 0, 10, 0));
        return container;
    }

    private JLabel buildTopBarUriButton(String iconPath, String tooltip, String uriString) {
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), iconPath);
        return buildButton(icon, tooltip, () -> {
            LinkBrowser.browse(uriString);
        });
    }

}
