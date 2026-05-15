package com.dopamineenhancer.triggers;

import com.dopamineenhancer.CelebrationController;
import com.dopamineenhancer.CelebrationType;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import com.dopamineenhancer.DopamineEnhancerConfig;

@Singleton
public class DopamineEnhancerTriggers
{
    private static final String COLLECTION_LOG_MESSAGE = "new item added to your collection log";

    private final DopamineEnhancerConfig config;
    private final CelebrationController celebrationController;

    @Inject
    DopamineEnhancerTriggers(DopamineEnhancerConfig config, CelebrationController celebrationController)
    {
        this.config = config;
        this.celebrationController = celebrationController;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        String message = Text.removeTags(event.getMessage()).toLowerCase();
        if (config.questCelebrations() && isQuestCompletionMessage(message))
        {
            celebrationController.celebrate(CelebrationType.QUEST);
            return;
        }

        if (config.collectionLogCelebrations() && message.contains(COLLECTION_LOG_MESSAGE))
        {
            celebrationController.celebrate(CelebrationType.COLLECTION_LOG);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!config.withdrawCelebrations())
        {
            return;
        }

        String option = Text.removeTags(event.getMenuOption()).toLowerCase();
        if (option.startsWith("withdraw"))
        {
            celebrationController.celebrate(CelebrationType.WITHDRAW);
        }
    }

    private boolean isQuestCompletionMessage(String message)
    {
        return message.contains("quest complete") || message.contains("you have completed");
    }
}
