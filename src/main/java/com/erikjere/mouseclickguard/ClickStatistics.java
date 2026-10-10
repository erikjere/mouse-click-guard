package com.erikjere.mouseclickguard;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import lombok.Value;

/**
 * Session, lifetime and daily statistics for all tracked buttons. Thread-safe: presses are recorded on the AWT
 * event thread while persistence and chat warnings run elsewhere.
 */
class ClickStatistics
{
	static final int RETENTION_DAYS = 90;
	static final int HEALTH_WINDOW_DAYS = 7;
	static final int RECENT_EVENTS = 50;

	private ButtonPair session = new ButtonPair();
	private ButtonPair lifetime = new ButtonPair();
	private final TreeMap<LocalDate, ButtonPair> daily = new TreeMap<>();
	/**
	 * Most recent Chatter presses, newest first.
	 */
	private final Deque<PressEvent> recent = new ArrayDeque<>();
	private boolean dirty;

	synchronized void record(PressEvent event, LocalDate today)
	{
		session.record(event);
		lifetime.record(event);
		daily.computeIfAbsent(today, d -> new ButtonPair()).record(event);
		if (event.isChatter())
		{
			recent.addFirst(event);
			while (recent.size() > RECENT_EVENTS)
			{
				recent.removeLast();
			}
		}
		prune(today);
		dirty = true;
	}

	/**
	 * Mouse health over the last {@link #HEALTH_WINDOW_DAYS} days, so the verdict follows the current state of the
	 * mouse rather than its whole history.
	 */
	synchronized MouseHealth health(MouseButton button, LocalDate today)
	{
		Counts window = new Counts();
		for (ButtonPair day : daily.subMap(today.minusDays(HEALTH_WINDOW_DAYS - 1), true, today, true).values())
		{
			window.add(day.get(button).getCounts());
		}
		return MouseHealth.of(window.getPresses(), window.getChatter());
	}

	synchronized void reset()
	{
		session = new ButtonPair();
		lifetime = new ButtonPair();
		daily.clear();
		recent.clear();
		dirty = true;
	}

	synchronized Snapshot snapshot(LocalDate today)
	{
		TreeMap<LocalDate, ButtonPair> dailyCopy = new TreeMap<>();
		daily.forEach((date, pair) -> dailyCopy.put(date, pair.copy()));
		return new Snapshot(
			session.copy(),
			lifetime.copy(),
			Collections.unmodifiableNavigableMap(dailyCopy.descendingMap()),
			Collections.unmodifiableList(new ArrayList<>(recent)),
			health(MouseButton.LEFT, today),
			health(MouseButton.RIGHT, today));
	}

	/**
	 * @return true if anything changed since the last call
	 */
	synchronized boolean takeDirty()
	{
		boolean wasDirty = dirty;
		dirty = false;
		return wasDirty;
	}

	synchronized Persisted toPersisted()
	{
		Persisted persisted = new Persisted();
		persisted.lifetime = lifetime.copy();
		daily.forEach((date, pair) -> persisted.daily.put(date.toString(), pair.copy()));
		return persisted;
	}

	synchronized void restore(Persisted persisted)
	{
		lifetime = persisted.lifetime == null ? new ButtonPair() : persisted.lifetime;
		lifetime.normalize();
		daily.clear();
		if (persisted.daily != null)
		{
			for (Map.Entry<String, ButtonPair> entry : persisted.daily.entrySet())
			{
				ButtonPair pair = entry.getValue();
				if (pair != null)
				{
					pair.normalize();
					daily.put(LocalDate.parse(entry.getKey()), pair);
				}
			}
		}
	}

	private void prune(LocalDate today)
	{
		daily.headMap(today.minusDays(RETENTION_DAYS - 1)).clear();
	}

	@Value
	static class Snapshot
	{
		ButtonPair session;
		ButtonPair lifetime;
		/**
		 * Newest day first.
		 */
		NavigableMap<LocalDate, ButtonPair> daily;
		List<PressEvent> recent;
		MouseHealth leftHealth;
		MouseHealth rightHealth;

		MouseHealth getHealth(MouseButton button)
		{
			return button == MouseButton.LEFT ? leftHealth : rightHealth;
		}
	}

	/**
	 * Stored form. The session is deliberately not persisted.
	 */
	static class Persisted
	{
		int version = 1;
		ButtonPair lifetime = new ButtonPair();
		Map<String, ButtonPair> daily = new TreeMap<>();
	}
}
