package com.erikjere.mouseclickguard;

import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.runelite.client.input.MouseAdapter;

/**
 * Feeds Presses to the {@link PressDetector}s and, for a Suppressed press, also consumes the release and click
 * events that belong to it so the game never sees half of a click. Only consumes events, never creates them.
 */
class ClickGuardMouseListener extends MouseAdapter
{
	private final IntSupplier thresholdMs;
	private final Predicate<MouseButton> preventionEnabled;
	private final Consumer<PressEvent> sink;

	private final Map<MouseButton, PressDetector> detectors = new EnumMap<>(MouseButton.class);
	private final Set<MouseButton> swallowRelease = EnumSet.noneOf(MouseButton.class);
	private final Set<MouseButton> swallowClick = EnumSet.noneOf(MouseButton.class);

	ClickGuardMouseListener(IntSupplier thresholdMs, Predicate<MouseButton> preventionEnabled, Consumer<PressEvent> sink)
	{
		this.thresholdMs = thresholdMs;
		this.preventionEnabled = preventionEnabled;
		this.sink = sink;
		for (MouseButton button : MouseButton.values())
		{
			detectors.put(button, new PressDetector(button));
		}
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		MouseButton button = MouseButton.of(event);
		if (button == null)
		{
			return event;
		}
		// A click event only follows a release when the mouse did not move, so a pending one may never arrive.
		swallowClick.remove(button);

		PressEvent press = detectors.get(button).onPress(event.getWhen(), thresholdMs.getAsInt(), preventionEnabled.test(button));
		sink.accept(press);
		if (press.isSuppressed())
		{
			swallowRelease.add(button);
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		MouseButton button = MouseButton.of(event);
		if (button != null && swallowRelease.remove(button))
		{
			swallowClick.add(button);
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		MouseButton button = MouseButton.of(event);
		if (button != null && swallowClick.remove(button))
		{
			event.consume();
		}
		return event;
	}
}
