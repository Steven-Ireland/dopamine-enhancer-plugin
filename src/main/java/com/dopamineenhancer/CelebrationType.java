package com.dopamineenhancer;

public enum CelebrationType
{
    QUEST("Quest complete!"),
    COLLECTION_LOG("Collection log!"),
    WITHDRAW("Withdrawn!");

    private final String defaultMessage;

    CelebrationType(String defaultMessage)
    {
        this.defaultMessage = defaultMessage;
    }

    String getDefaultMessage()
    {
        return defaultMessage;
    }
}
