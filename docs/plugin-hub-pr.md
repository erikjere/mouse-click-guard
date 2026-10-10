Adds **Mouse Click Guard**, a plugin that helps players find out whether their mouse has a failing switch.

A worn switch "chatters": one physical press arrives as two presses a few milliseconds apart. The plugin
times the interval between presses of the left and right button inside the game canvas and shows the results
in a side panel (interval histogram, chatter per 1,000 presses, a Healthy / Suspect / Failing verdict and a
daily history). Optional chat messages summarise chatter, throttled to once every 5 minutes per button.

### Click prevention (opt-in, off by default)

Users can turn on a per-button setting that discards a press arriving sooner than the configured threshold
(10–150 ms, default 50 ms) after the last press that reached the game, together with that press's release and
click events.

Notes on compliance:

- It only **consumes** events through `MouseManager`. It never creates, injects, delays, moves or re-times
  input, and never sends anything to the server.
- It can only ever *remove* a press the hardware generated within the threshold of a previous one, i.e. it
  makes the client behave like a mouse with a working switch. It cannot make anything faster or easier
  than a functioning mouse would.
- Suppression is measured from the last accepted press, so it can never swallow input for longer than one
  threshold window.
- No menu entries are added, swapped or removed; no click zones are changed.

### Other

- No third-party dependencies, no network access, no file I/O. Statistics are stored via `ConfigManager`.
- Unit tests cover the detection, suppression and statistics logic.

Generated-by: Claude Code

🤖 Generated with [Claude Code](https://claude.com/claude-code)
