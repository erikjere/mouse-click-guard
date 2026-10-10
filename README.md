# Mouse Click Guard

A worn mouse switch can "chatter": you press the button once, but the computer sees two presses a few
milliseconds apart. In game that looks like a random double click — a dropped item, a cancelled action,
a doubled prayer flick. Mouse Click Guard measures the time between your presses so you can see whether your
mouse is failing, and can optionally discard the phantom second press.

## Features

- **Chatter detection** for the left and right button. A press that follows the previous press of the same
  button sooner than the *chatter threshold* (default 50 ms) is counted as chatter. Deliberate fast clicking,
  including prayer flicking, is normally well above 80 ms.
- **Mouse health** per button — *Healthy*, *Suspect* or *Failing* — based on chatter per 1,000 presses over the
  last 7 days (shown once at least 500 presses have been seen).
- **Side panel** with a histogram of press intervals (chatter range in red), session and lifetime totals,
  recent chatter events and a 90-day daily history.
- **Chat warnings** (optional): a summary at most once every 5 minutes per button, and a message whenever a
  button's health changes.
- **Click prevention** (optional, off by default, separate toggle per button): a press that comes sooner than
  the threshold after the last press that reached the game is discarded, together with its release.
  Prevention only *discards* duplicate presses; it never creates, delays or alters input.

Only clicks inside the game area are measured; RuneLite's own side panels are not visible to plugins.
Statistics are stored in your RuneLite profile and are never sent anywhere else.

## Settings

| Setting | Default | |
|---|---|---|
| Chatter threshold | 50 ms | 10–150 ms |
| Prevent left chatter | off | |
| Prevent right chatter | off | |
| Chat warnings | on | |

## Development

Requires JDK 11.

```sh
./gradlew test   # unit tests
./gradlew run    # launch a RuneLite dev client with the plugin loaded
```

Releasing: bump `version` in `runelite-plugin.properties`, commit, then push a tag `v<version>`.
The *Release* workflow opens (or updates) the pull request to
[runelite/plugin-hub](https://github.com/runelite/plugin-hub). See `scripts/submit-to-plugin-hub.sh`.
