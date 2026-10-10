package com.erikjere.mouseclickguard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PressDetectorTest
{
	private static final int THRESHOLD = 50;

	private final PressDetector detector = new PressDetector(MouseButton.LEFT);

	@Test
	public void firstPressHasNoIntervalAndIsNotChatter()
	{
		PressEvent e = detector.onPress(1_000, THRESHOLD, true);

		assertEquals(PressEvent.NO_INTERVAL, e.getIntervalMs());
		assertFalse(e.isChatter());
		assertFalse(e.isSuppressed());
	}

	@Test
	public void pressBelowThresholdIsChatter()
	{
		detector.onPress(1_000, THRESHOLD, false);
		PressEvent e = detector.onPress(1_020, THRESHOLD, false);

		assertEquals(20, e.getIntervalMs());
		assertTrue(e.isChatter());
	}

	@Test
	public void pressAtThresholdIsNotChatter()
	{
		detector.onPress(1_000, THRESHOLD, false);
		PressEvent e = detector.onPress(1_050, THRESHOLD, false);

		assertFalse(e.isChatter());
	}

	@Test
	public void chatterIsNotSuppressedWhenPreventionIsOff()
	{
		detector.onPress(1_000, THRESHOLD, false);
		PressEvent e = detector.onPress(1_010, THRESHOLD, false);

		assertTrue(e.isChatter());
		assertFalse(e.isSuppressed());
	}

	@Test
	public void chatterIsSuppressedWhenPreventionIsOn()
	{
		detector.onPress(1_000, THRESHOLD, true);
		PressEvent e = detector.onPress(1_010, THRESHOLD, true);

		assertTrue(e.isSuppressed());
	}

	@Test
	public void suppressionIsMeasuredFromLastAcceptedPress()
	{
		// Presses every 20 ms: 0 accepted, 20 and 40 suppressed, 60 accepted (60 ms after 0), 80 suppressed.
		assertFalse(detector.onPress(0, THRESHOLD, true).isSuppressed());
		assertTrue(detector.onPress(20, THRESHOLD, true).isSuppressed());
		assertTrue(detector.onPress(40, THRESHOLD, true).isSuppressed());

		PressEvent sixty = detector.onPress(60, THRESHOLD, true);
		assertTrue("raw interval is 20 ms so it is still chatter", sixty.isChatter());
		assertFalse(sixty.isSuppressed());

		assertTrue(detector.onPress(80, THRESHOLD, true).isSuppressed());
	}

	@Test
	public void statisticsUseRawIntervalEvenAfterSuppression()
	{
		detector.onPress(0, THRESHOLD, true);
		detector.onPress(20, THRESHOLD, true);
		PressEvent e = detector.onPress(45, THRESHOLD, true);

		assertEquals(25, e.getIntervalMs());
	}

	@Test
	public void clockGoingBackwardsYieldsNoInterval()
	{
		detector.onPress(1_000, THRESHOLD, true);
		PressEvent e = detector.onPress(900, THRESHOLD, true);

		assertEquals(PressEvent.NO_INTERVAL, e.getIntervalMs());
		assertFalse(e.isChatter());
		assertFalse(e.isSuppressed());
	}
}
