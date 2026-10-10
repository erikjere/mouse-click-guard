package com.erikjere.mouseclickguard;

import java.awt.event.MouseEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum MouseButton
{
	LEFT("left"),
	RIGHT("right");

	private final String displayName;

	/**
	 * @return the tracked button for this event, or null for buttons that are not tracked
	 */
	static MouseButton of(MouseEvent event)
	{
		switch (event.getButton())
		{
			case MouseEvent.BUTTON1:
				return LEFT;
			case MouseEvent.BUTTON3:
				return RIGHT;
			default:
				return null;
		}
	}
}
