package com.erikjere.mouseclickguard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which chat warnings to show: a Chatter summary per button at most once per {@link #THROTTLE_MS}, and a
 * message whenever a button's Mouse health changes to a known verdict.
 */
class ChatWarnings
{
	static final long THROTTLE_MS = 5 * 60_000;

	private final Map<MouseButton, Counts> pending = new EnumMap<>(MouseButton.class);
	private final Map<MouseButton, Long> lastWarning = new EnumMap<>(MouseButton.class);
	private final Map<MouseButton, MouseHealth> health = new EnumMap<>(MouseButton.class);
	private final List<String> healthMessages = new ArrayList<>();

	synchronized void onPress(PressEvent event)
	{
		if (event.isChatter())
		{
			pending.computeIfAbsent(event.getButton(), b -> new Counts()).record(event);
		}
	}

	synchronized void onHealth(MouseButton button, MouseHealth current)
	{
		MouseHealth previous = health.put(button, current);
		if (previous != null && previous != current && current != MouseHealth.UNKNOWN)
		{
			healthMessages.add(String.format("Mouse Click Guard: %s button health changed from %s to %s.",
				button.getDisplayName(), previous.getDisplayName(), current.getDisplayName()));
		}
	}

	synchronized List<String> poll(long now)
	{
		List<String> messages = new ArrayList<>(healthMessages);
		healthMessages.clear();
		for (MouseButton button : MouseButton.values())
		{
			Counts counts = pending.get(button);
			Long last = lastWarning.get(button);
			if (counts == null || (last != null && now - last < THROTTLE_MS))
			{
				continue;
			}
			messages.add(String.format("Mouse Click Guard: %d chatter press%s on the %s button (%d suppressed).",
				counts.getChatter(), counts.getChatter() == 1 ? "" : "es", button.getDisplayName(), counts.getSuppressed()));
			pending.remove(button);
			lastWarning.put(button, now);
		}
		return messages;
	}

	synchronized void clearPending()
	{
		pending.clear();
	}
}
