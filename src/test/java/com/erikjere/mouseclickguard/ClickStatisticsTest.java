package com.erikjere.mouseclickguard;

import static org.junit.Assert.assertEquals;
import java.time.LocalDate;
import org.junit.Test;

public class ClickStatisticsTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	private final ClickStatistics stats = new ClickStatistics();

	private static PressEvent press(MouseButton button, long interval, boolean chatter, boolean suppressed)
	{
		return new PressEvent(button, 0, interval, chatter, suppressed);
	}

	@Test
	public void countsArePerButton()
	{
		stats.record(press(MouseButton.LEFT, 100, false, false), TODAY);
		stats.record(press(MouseButton.LEFT, 10, true, true), TODAY);
		stats.record(press(MouseButton.RIGHT, 10, true, false), TODAY);

		Counts left = stats.snapshot(TODAY).getSession().get(MouseButton.LEFT).getCounts();
		assertEquals(2, left.getPresses());
		assertEquals(1, left.getChatter());
		assertEquals(1, left.getSuppressed());

		Counts right = stats.snapshot(TODAY).getLast7Days().get(MouseButton.RIGHT).getCounts();
		assertEquals(1, right.getPresses());
		assertEquals(0, right.getSuppressed());
	}

	@Test
	public void histogramBinsByTenMillisecondsWithOverflow()
	{
		stats.record(press(MouseButton.LEFT, 0, true, false), TODAY);
		stats.record(press(MouseButton.LEFT, 19, true, false), TODAY);
		stats.record(press(MouseButton.LEFT, 5_000, false, false), TODAY);
		stats.record(press(MouseButton.LEFT, PressEvent.NO_INTERVAL, false, false), TODAY);

		long[] histogram = stats.snapshot(TODAY).getSession().get(MouseButton.LEFT).getHistogram();
		assertEquals(1, histogram[0]);
		assertEquals(1, histogram[1]);
		assertEquals(1, histogram[ButtonStats.BINS - 1]);
	}

	@Test
	public void healthUsesTheLastSevenDays()
	{
		LocalDate eightDaysAgo = TODAY.minusDays(7);
		for (int i = 0; i < 1_000; i++)
		{
			stats.record(press(MouseButton.LEFT, 10, true, false), eightDaysAgo);
		}
		for (int i = 0; i < 1_000; i++)
		{
			stats.record(press(MouseButton.LEFT, 200, false, false), TODAY);
		}

		assertEquals(MouseHealth.HEALTHY, stats.health(MouseButton.LEFT, TODAY));
	}

	@Test
	public void windowsSumTheirDaysIncludingHistograms()
	{
		stats.record(press(MouseButton.LEFT, 15, true, false), TODAY);
		stats.record(press(MouseButton.LEFT, 15, true, false), TODAY.minusDays(6));
		stats.record(press(MouseButton.LEFT, 15, true, false), TODAY.minusDays(7));
		stats.record(press(MouseButton.LEFT, 15, true, false), TODAY.minusDays(89));

		ClickStatistics.Snapshot snapshot = stats.snapshot(TODAY);
		ButtonStats week = snapshot.getLast7Days().get(MouseButton.LEFT);
		assertEquals(2, week.getCounts().getPresses());
		assertEquals(2, week.getHistogram()[1]);
		assertEquals(4, snapshot.getLast90Days().get(MouseButton.LEFT).getCounts().getChatter());
	}

	@Test
	public void legacyLifetimeFieldIsIgnored()
	{
		ClickStatistics.Persisted persisted = new com.google.gson.Gson().fromJson(
			"{\"version\":1,\"lifetime\":{\"left\":{\"counts\":{\"presses\":9}}},\"daily\":{}}",
			ClickStatistics.Persisted.class);
		stats.restore(persisted);

		assertEquals(0, stats.snapshot(TODAY).getLast90Days().get(MouseButton.LEFT).getCounts().getPresses());
	}

	@Test
	public void daysOlderThanRetentionArePruned()
	{
		stats.record(press(MouseButton.LEFT, 100, false, false), TODAY.minusDays(ClickStatistics.RETENTION_DAYS));
		stats.record(press(MouseButton.LEFT, 100, false, false), TODAY.minusDays(ClickStatistics.RETENTION_DAYS - 1));
		stats.record(press(MouseButton.LEFT, 100, false, false), TODAY);

		assertEquals(2, stats.snapshot(TODAY).getDaily().size());
	}

	@Test
	public void persistedFormRoundTrips()
	{
		stats.record(press(MouseButton.RIGHT, 10, true, true), TODAY);

		ClickStatistics restored = new ClickStatistics();
		restored.restore(stats.toPersisted());

		ClickStatistics.Snapshot snapshot = restored.snapshot(TODAY);
		assertEquals(1, snapshot.getLast7Days().get(MouseButton.RIGHT).getCounts().getSuppressed());
		assertEquals(1, snapshot.getDaily().get(TODAY).get(MouseButton.RIGHT).getCounts().getChatter());
		assertEquals("session is not persisted", 0, snapshot.getSession().get(MouseButton.RIGHT).getCounts().getPresses());
	}

	@Test
	public void resetClearsEverything()
	{
		stats.record(press(MouseButton.LEFT, 10, true, true), TODAY);
		stats.reset();

		ClickStatistics.Snapshot snapshot = stats.snapshot(TODAY);
		assertEquals(0, snapshot.getLast90Days().get(MouseButton.LEFT).getCounts().getPresses());
		assertEquals(0, snapshot.getDaily().size());
		assertEquals(0, snapshot.getRecent().size());
	}
}
