package com.erikjere.mouseclickguard;

import lombok.Value;

/**
 * One Press of a tracked button, classified by {@link PressDetector}.
 */
@Value
class PressEvent
{
	static final long NO_INTERVAL = -1;

	MouseButton button;
	long timestamp;
	/**
	 * Press interval in ms since the previous Press of the same button, or {@link #NO_INTERVAL}.
	 */
	long intervalMs;
	boolean chatter;
	boolean suppressed;

	boolean hasInterval()
	{
		return intervalMs != NO_INTERVAL;
	}
}
