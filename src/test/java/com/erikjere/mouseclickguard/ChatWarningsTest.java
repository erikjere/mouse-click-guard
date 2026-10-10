package com.erikjere.mouseclickguard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class ChatWarningsTest
{
	private static final long MINUTE = 60_000;

	private final ChatWarnings warnings = new ChatWarnings();

	private static PressEvent chatter(MouseButton button, boolean suppressed)
	{
		return new PressEvent(button, 0, 10, true, suppressed);
	}

	@Test
	public void nothingToSayWithoutChatter()
	{
		assertTrue(warnings.poll(0).isEmpty());
	}

	@Test
	public void firstChatterIsReportedImmediately()
	{
		warnings.onPress(chatter(MouseButton.LEFT, true));

		List<String> messages = warnings.poll(0);
		assertEquals(1, messages.size());
		assertEquals("Mouse Click Guard: 1 chatter press on the left button (1 suppressed).", messages.get(0));
	}

	@Test
	public void furtherChatterWaitsForTheThrottleWindow()
	{
		warnings.onPress(chatter(MouseButton.LEFT, false));
		warnings.poll(0);

		warnings.onPress(chatter(MouseButton.LEFT, false));
		warnings.onPress(chatter(MouseButton.LEFT, true));
		assertTrue(warnings.poll(4 * MINUTE).isEmpty());

		List<String> messages = warnings.poll(5 * MINUTE);
		assertEquals("Mouse Click Guard: 2 chatter presses on the left button (1 suppressed).", messages.get(0));
	}

	@Test
	public void buttonsAreReportedSeparately()
	{
		warnings.onPress(chatter(MouseButton.LEFT, false));
		warnings.onPress(chatter(MouseButton.RIGHT, false));

		assertEquals(2, warnings.poll(0).size());
	}

	@Test
	public void nonChatterPressesAreIgnored()
	{
		warnings.onPress(new PressEvent(MouseButton.LEFT, 0, 200, false, false));

		assertTrue(warnings.poll(0).isEmpty());
	}

	@Test
	public void healthChangeIsAnnouncedOnce()
	{
		warnings.onHealth(MouseButton.LEFT, MouseHealth.HEALTHY);
		assertTrue("initial value is a baseline, not a change", warnings.poll(0).isEmpty());

		warnings.onHealth(MouseButton.LEFT, MouseHealth.SUSPECT);
		warnings.onHealth(MouseButton.LEFT, MouseHealth.SUSPECT);

		List<String> messages = warnings.poll(0);
		assertEquals(1, messages.size());
		assertEquals("Mouse Click Guard: left button health changed from Healthy to Suspect.", messages.get(0));
	}

	@Test
	public void changesFromUnknownAreAnnouncedButChangesToUnknownAreNot()
	{
		warnings.onHealth(MouseButton.LEFT, MouseHealth.UNKNOWN);
		warnings.onHealth(MouseButton.LEFT, MouseHealth.FAILING);
		assertEquals(1, warnings.poll(0).size());

		warnings.onHealth(MouseButton.LEFT, MouseHealth.UNKNOWN);
		assertTrue(warnings.poll(0).isEmpty());
	}
}
