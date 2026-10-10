package com.erikjere.mouseclickguard;

import lombok.Getter;

@Getter
class Counts
{
	private long presses;
	private long chatter;
	private long suppressed;

	void record(PressEvent event)
	{
		presses++;
		if (event.isChatter())
		{
			chatter++;
		}
		if (event.isSuppressed())
		{
			suppressed++;
		}
	}

	void add(Counts other)
	{
		presses += other.presses;
		chatter += other.chatter;
		suppressed += other.suppressed;
	}

	Counts copy()
	{
		Counts copy = new Counts();
		copy.add(this);
		return copy;
	}
}
