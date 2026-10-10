package com.erikjere.mouseclickguard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum MouseHealth
{
	UNKNOWN("Unknown"),
	HEALTHY("Healthy"),
	SUSPECT("Suspect"),
	FAILING("Failing");

	static final long MIN_PRESSES = 500;
	private static final double SUSPECT_PER_THOUSAND = 1;
	private static final double FAILING_PER_THOUSAND = 10;

	private final String displayName;

	static MouseHealth of(long presses, long chatter)
	{
		if (presses < MIN_PRESSES)
		{
			return UNKNOWN;
		}
		double rate = chatterRate(presses, chatter);
		if (rate > FAILING_PER_THOUSAND)
		{
			return FAILING;
		}
		if (rate >= SUSPECT_PER_THOUSAND)
		{
			return SUSPECT;
		}
		return HEALTHY;
	}

	/**
	 * @return Chatter presses per 1,000 presses
	 */
	static double chatterRate(long presses, long chatter)
	{
		return presses == 0 ? 0 : chatter * 1000.0 / presses;
	}
}
