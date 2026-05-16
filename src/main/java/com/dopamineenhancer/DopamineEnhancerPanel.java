package com.dopamineenhancer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Singleton
class DopamineEnhancerPanel extends PluginPanel
{
    private final ClientThread clientThread;
    private final CelebrationController celebrationController;
    private final List<JToggleButton> toggleButtons = new ArrayList<>();

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

        content.add(createToggleButton("Toggle quest", CelebrationType.QUEST));
        content.add(createToggleButton("Toggle collection log", CelebrationType.COLLECTION_LOG));
        content.add(createToggleButton("Toggle withdraw", CelebrationType.WITHDRAW));

        add(title, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JToggleButton createToggleButton(String label, CelebrationType type)
    {
        JToggleButton button = new JToggleButton(label);
        toggleButtons.add(button);
        button.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 32));
        button.addActionListener(event ->
        {
            boolean selected = button.isSelected();
            if (selected)
            {
                toggleButtons.stream()
                    .filter(toggleButton -> toggleButton != button)
                    .forEach(toggleButton -> toggleButton.setSelected(false));
            }

            clientThread.invoke(() -> celebrationController.toggle(type, selected));
        });
        return button;
    }
}
