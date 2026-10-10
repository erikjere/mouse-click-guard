package com.erikjere.mouseclickguard;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MouseHealthTest
{
	@Test
	public void unknownBelowMinimumPresses()
	{
		assertEquals(MouseHealth.UNKNOWN, MouseHealth.of(499, 400));
	}

	@Test
	public void healthyBelowOnePerThousand()
	{
		assertEquals(MouseHealth.HEALTHY, MouseHealth.of(2_000, 1));
	}

	@Test
	public void suspectFromOnePerThousand()
	{
		assertEquals(MouseHealth.SUSPECT, MouseHealth.of(1_000, 1));
	}

	@Test
	public void suspectUpToTenPerThousand()
	{
		assertEquals(MouseHealth.SUSPECT, MouseHealth.of(1_000, 10));
	}

	@Test
	public void failingAboveTenPerThousand()
	{
		assertEquals(MouseHealth.FAILING, MouseHealth.of(1_000, 11));
	}
}
