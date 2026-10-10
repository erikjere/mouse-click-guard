package com.erikjere.mouseclickguard;

/**
 * Classifies the Presses of a single button as Chatter and decides whether Click prevention suppresses them.
 * Chatter is judged on the raw Press interval; suppression is judged against the last Accepted press, so a burst
 * of chatter can never swallow presses for longer than one threshold window.
 */
class PressDetector
{
	private static final long NONE = Long.MIN_VALUE;

	private final MouseButton button;
	private long lastPress = NONE;
	private long lastAccepted = NONE;

	PressDetector(MouseButton button)
	{
		this.button = button;
	}

	PressEvent onPress(long when, int thresholdMs, boolean prevent)
	{
		long interval = elapsed(lastPress, when);
		long sinceAccepted = elapsed(lastAccepted, when);

		boolean chatter = interval != PressEvent.NO_INTERVAL && interval < thresholdMs;
		boolean suppressed = prevent && sinceAccepted != PressEvent.NO_INTERVAL && sinceAccepted < thresholdMs;

		lastPress = when;
		if (!suppressed)
		{
			lastAccepted = when;
		}
		return new PressEvent(button, when, interval, chatter, suppressed);
	}

	private static long elapsed(long from, long to)
	{
		if (from == NONE || to < from)
		{
			return PressEvent.NO_INTERVAL;
		}
		return to - from;
	}
}
