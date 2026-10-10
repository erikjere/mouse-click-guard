package com.erikjere.mouseclickguard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Canvas;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ClickGuardMouseListenerTest
{
	private static final Canvas CANVAS = new Canvas();

	private final List<PressEvent> recorded = new ArrayList<>();
	private boolean prevent = true;
	private final ClickGuardMouseListener listener = new ClickGuardMouseListener(
		() -> 50,
		button -> prevent,
		recorded::add);

	private static MouseEvent event(int id, long when, int button)
	{
		return new MouseEvent(CANVAS, id, when, 0, 10, 10, 1, false, button);
	}

	private MouseEvent press(long when)
	{
		return listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, when, MouseEvent.BUTTON1));
	}

	private MouseEvent release(long when)
	{
		return listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, when, MouseEvent.BUTTON1));
	}

	private MouseEvent click(long when)
	{
		return listener.mouseClicked(event(MouseEvent.MOUSE_CLICKED, when, MouseEvent.BUTTON1));
	}

	@Test
	public void suppressedPressAndItsReleaseAndClickAreConsumed()
	{
		assertFalse(press(0).isConsumed());
		assertFalse(release(5).isConsumed());
		assertFalse(click(5).isConsumed());

		assertTrue(press(15).isConsumed());
		assertTrue(release(90).isConsumed());
		assertTrue(click(90).isConsumed());

		assertFalse("next real press is untouched", press(300).isConsumed());
		assertFalse(release(350).isConsumed());
		assertFalse(click(350).isConsumed());
	}

	@Test
	public void missingClickEventDoesNotLeakIntoTheNextPress()
	{
		press(0);
		release(5);
		press(15);
		release(90);
		// no mouseClicked because the mouse moved

		assertFalse(press(300).isConsumed());
		assertFalse(release(350).isConsumed());
		assertFalse(click(350).isConsumed());
	}

	@Test
	public void nothingIsConsumedWhenPreventionIsOff()
	{
		prevent = false;
		press(0);
		release(5);

		assertFalse(press(15).isConsumed());
		assertFalse(release(90).isConsumed());
		assertTrue(recorded.get(1).isChatter());
	}

	@Test
	public void middleButtonIsIgnored()
	{
		listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON2));

		assertTrue(recorded.isEmpty());
	}

	@Test
	public void buttonsAreTimedIndependently()
	{
		press(0);
		MouseEvent right = listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 10, MouseEvent.BUTTON3));

		assertFalse(right.isConsumed());
		assertEquals(MouseButton.RIGHT, recorded.get(1).getButton());
		assertEquals(PressEvent.NO_INTERVAL, recorded.get(1).getIntervalMs());
	}
}
