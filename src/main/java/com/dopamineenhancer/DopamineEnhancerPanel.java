package com.dopamineenhancer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Singleton
class DopamineEnhancerPanel extends PluginPanel
{
    private final ClientThread clientThread;
    private final CelebrationController celebrationController;

    @Inject
    DopamineEnhancerPanel(ClientThread clientThread, CelebrationController celebrationController)
    {
        this.clientThread = clientThread;
        this.celebrationController = celebrationController;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Dopamine Enhancer");
        title.setForeground(Color.WHITE);

        JPanel content = new JPanel(new GridLayout(0, 1, 0, 8));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(new EmptyBorder(10, 0, 0, 0));

        content.add(createSimulateButton("Simulate quest trigger", CelebrationType.QUEST));
        content.add(createSimulateButton("Simulate collection log trigger", CelebrationType.COLLECTION_LOG));
        content.add(createSimulateButton("Simulate withdraw trigger", CelebrationType.WITHDRAW));

        add(title, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JButton createSimulateButton(String label, CelebrationType type)
    {
        JButton button = new JButton(label);
        button.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 32));
        button.addActionListener(event -> clientThread.invoke(() -> celebrationController.celebrate(type)));
        return button;
    }
}
