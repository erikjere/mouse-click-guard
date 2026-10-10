package com.erikjere.mouseclickguard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class MouseClickGuardPanel extends PluginPanel
{
	private static final String LAST_7_DAYS = "Last " + ClickStatistics.HEALTH_WINDOW_DAYS + " days";
	private static final String LAST_90_DAYS = "Last " + ClickStatistics.RETENTION_DAYS + " days";
	private static final String SESSION = "This session";
	private static final int RECENT_SHOWN = 20;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM");

	private final JComboBox<String> period = new JComboBox<>(new String[]{LAST_7_DAYS, LAST_90_DAYS, SESSION});
	private final Map<MouseButton, ButtonSection> sections = new EnumMap<>(MouseButton.class);
	private final JPanel recent = verticalPanel();
	private final JPanel daily = new JPanel(new GridLayout(0, 3, 4, 2));

	private ClickStatistics.Snapshot snapshot;
	private int thresholdMs;

	MouseClickGuardPanel(Runnable onReset)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Mouse Click Guard");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		add(row(title));

		period.addActionListener(e -> render());
		add(row(period));

		for (MouseButton button : MouseButton.values())
		{
			ButtonSection section = new ButtonSection(button);
			sections.put(button, section);
			add(section);
		}

		add(heading("Recent chatter"));
		add(recent);

		add(heading("Daily chatter per 1,000 presses"));
		daily.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(daily);

		JButton reset = new JButton("Reset statistics");
		reset.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this,
				"Delete all Mouse Click Guard statistics, including history?",
				"Reset statistics", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				onReset.run();
			}
		});
		add(row(reset));
	}

	void update(ClickStatistics.Snapshot snapshot, int thresholdMs)
	{
		this.snapshot = snapshot;
		this.thresholdMs = thresholdMs;
		render();
	}

	private void render()
	{
		if (snapshot == null)
		{
			return;
		}
		ButtonPair stats = selectedPeriod();
		for (MouseButton button : MouseButton.values())
		{
			sections.get(button).update(stats.get(button), snapshot.getHealth(button), thresholdMs);
		}

		recent.removeAll();
		List<PressEvent> events = snapshot.getRecent();
		if (events.isEmpty())
		{
			recent.add(small("None yet", ColorScheme.LIGHT_GRAY_COLOR));
		}
		for (PressEvent event : events.subList(0, Math.min(RECENT_SHOWN, events.size())))
		{
			String text = String.format("%s  %s  %d ms%s", TIME.format(Instant.ofEpochMilli(event.getTimestamp())),
				capitalize(event.getButton().getDisplayName()), event.getIntervalMs(), event.isSuppressed() ? "  suppressed" : "");
			recent.add(small(text, event.isSuppressed() ? ColorScheme.PROGRESS_ERROR_COLOR : Color.WHITE));
		}

		daily.removeAll();
		daily.add(small("Day", ColorScheme.LIGHT_GRAY_COLOR));
		daily.add(small("Left", ColorScheme.LIGHT_GRAY_COLOR));
		daily.add(small("Right", ColorScheme.LIGHT_GRAY_COLOR));
		for (Map.Entry<LocalDate, ButtonPair> day : snapshot.getDaily().entrySet())
		{
			daily.add(small(DAY.format(day.getKey()), Color.WHITE));
			for (MouseButton button : MouseButton.values())
			{
				Counts counts = day.getValue().get(button).getCounts();
				daily.add(small(counts.getPresses() == 0 ? "-"
					: String.format("%.1f (%d)", MouseHealth.chatterRate(counts.getPresses(), counts.getChatter()), counts.getPresses()),
					Color.WHITE));
			}
		}

		revalidate();
		repaint();
	}

	private ButtonPair selectedPeriod()
	{
		Object selected = period.getSelectedItem();
		if (LAST_90_DAYS.equals(selected))
		{
			return snapshot.getLast90Days();
		}
		if (SESSION.equals(selected))
		{
			return snapshot.getSession();
		}
		return snapshot.getLast7Days();
	}

	private static class ButtonSection extends JPanel
	{
		private final JLabel health = new JLabel();
		private final JLabel counts = new JLabel();
		private final IntervalHistogram histogram = new IntervalHistogram();

		ButtonSection(MouseButton button)
		{
			setLayout(new BorderLayout(0, 4));
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(6, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(6, 6, 6, 6)));

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			JLabel name = new JLabel(capitalize(button.getDisplayName()) + " button");
			name.setForeground(Color.WHITE);
			header.add(name, BorderLayout.WEST);
			health.setToolTipText("Based on the last " + ClickStatistics.HEALTH_WINDOW_DAYS + " days; needs "
				+ MouseHealth.MIN_PRESSES + " presses");
			header.add(health, BorderLayout.EAST);

			counts.setFont(FontManager.getRunescapeSmallFont());
			counts.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

			add(header, BorderLayout.NORTH);
			add(counts, BorderLayout.CENTER);
			add(histogram, BorderLayout.SOUTH);
		}

		void update(ButtonStats stats, MouseHealth verdict, int thresholdMs)
		{
			health.setText(verdict.getDisplayName());
			health.setForeground(color(verdict));
			Counts c = stats.getCounts();
			counts.setText(String.format("<html>%d presses, %d chatter (%.1f / 1,000)<br>%d suppressed</html>",
				c.getPresses(), c.getChatter(), MouseHealth.chatterRate(c.getPresses(), c.getChatter()), c.getSuppressed()));
			histogram.update(stats.getHistogram(), thresholdMs);
		}

		private static Color color(MouseHealth verdict)
		{
			switch (verdict)
			{
				case HEALTHY:
					return ColorScheme.PROGRESS_COMPLETE_COLOR;
				case SUSPECT:
					return ColorScheme.PROGRESS_INPROGRESS_COLOR;
				case FAILING:
					return ColorScheme.PROGRESS_ERROR_COLOR;
				default:
					return ColorScheme.LIGHT_GRAY_COLOR;
			}
		}
	}

	private static JPanel verticalPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return panel;
	}

	private static JPanel row(java.awt.Component component)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 0, 4, 0));
		row.add(component, BorderLayout.CENTER);
		return row;
	}

	private static JPanel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		JPanel row = row(label);
		row.setBorder(new EmptyBorder(10, 0, 4, 0));
		return row;
	}

	private static JLabel small(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	private static String capitalize(String text)
	{
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}
