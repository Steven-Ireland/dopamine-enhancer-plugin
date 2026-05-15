# Dopamine Enhancer

A RuneLite external plugin scaffold for adding small visual and audio rewards when you:

- complete quests
- receive collection log notifications
- withdraw items from bank-style interfaces

## Development

This follows RuneLite's external plugin template. With Gradle available locally:

```sh
gradle run
```

That launches RuneLite in developer mode and loads `DopamineEnhancerPlugin`.

## Notes

- Trigger detection is intentionally conservative and lives in `DopamineEnhancerTriggers`.
- Visual and sound playback are handled by `CelebrationController`.
- The overlay is a lightweight first pass so richer effects can be added later without changing event detection.
