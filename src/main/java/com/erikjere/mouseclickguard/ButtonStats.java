package com.erikjere.mouseclickguard;

/**
 * Counts and a Press interval histogram for one button over one period.
 */
class ButtonStats
{
	static final int BIN_WIDTH_MS = 10;
	/**
	 * 20 bins of 10 ms covering 0-199 ms, plus one overflow bin.
	 */
	static final int BINS = 21;

	private Counts counts = new Counts();
	private long[] histogram = new long[BINS];

	void record(PressEvent event)
	{
		counts.record(event);
		if (event.hasInterval())
		{
			histogram[(int) Math.min(event.getIntervalMs() / BIN_WIDTH_MS, BINS - 1)]++;
		}
	}

	Counts getCounts()
	{
		return counts;
	}

	long[] getHistogram()
	{
		return histogram;
	}

	ButtonStats copy()
	{
		ButtonStats copy = new ButtonStats();
		copy.counts = counts.copy();
		copy.histogram = histogram.clone();
		return copy;
	}

	/**
	 * Repairs instances deserialized from older or hand-edited data.
	 */
	void normalize()
	{
		if (counts == null)
		{
			counts = new Counts();
		}
		if (histogram == null || histogram.length != BINS)
		{
			histogram = new long[BINS];
		}
	}
}
