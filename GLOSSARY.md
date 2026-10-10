# Mouse Click Guard

A RuneLite plugin that watches the timing of mouse clicks to reveal a failing mouse switch, and can optionally discard the phantom clicks such a switch produces.

## Language

**Press**:
A single mouse-button-down event for one tracked button (left or right), as received by the game client.
_Avoid_: Click (ambiguous between press, release and full click), tap

**Accepted press**:
A Press that reached the game, i.e. was not a Suppressed press.

**Press interval**:
The time between two consecutive Presses of the same button.
_Avoid_: Click delay, gap, latency

**Chatter**:
A Press whose Press interval is below the Chatter threshold, the signature of a worn mouse switch that registers one physical press as several.
_Avoid_: Double click (that is also a deliberate user action), misclick, ghost click

**Chatter threshold**:
The Press interval below which a Press is considered Chatter.
_Avoid_: Debounce time, delay

**Click prevention**:
The user-toggled setting under which a Chatter Press is discarded before the game sees it.
_Avoid_: Auto-prevent, blocking, debounce mode

**Suppressed press**:
A Chatter Press that Click prevention discarded.
_Avoid_: Blocked click, eaten click

**Chatter rate**:
Chatter Presses per 1,000 Presses of one button over a given period.

**Mouse health**:
The per-button verdict derived from the Chatter rate: Healthy, Suspect or Failing; unknown until enough Presses have been seen.
_Avoid_: Score, status
