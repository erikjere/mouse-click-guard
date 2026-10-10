package com.erikjere.mouseclickguard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Bar chart of Press intervals; bars below the Chatter threshold are drawn in red.
 */
class IntervalHistogram extends JComponent
{
	private static final int HEIGHT = 70;
	private static final int AXIS_HEIGHT = 14;
	private static final Color CHATTER_COLOR = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final Color NORMAL_COLOR = ColorScheme.LIGHT_GRAY_COLOR;

	private long[] bins = new long[ButtonStats.BINS];
	private int thresholdMs;

	IntervalHistogram()
	{
		setPreferredSize(new Dimension(0, HEIGHT));
		setFont(FontManager.getRunescapeSmallFont());
		setToolTipText("");
	}

	void update(long[] bins, int thresholdMs)
	{
		this.bins = bins;
		this.thresholdMs = thresholdMs;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(ColorScheme.DARKER_GRAY_COLOR);
		g.fillRect(0, 0, getWidth(), getHeight());

		long max = 1;
		for (long bin : bins)
		{
			max = Math.max(max, bin);
		}

		int chartHeight = getHeight() - AXIS_HEIGHT;
		double barWidth = (double) getWidth() / bins.length;
		for (int i = 0; i < bins.length; i++)
		{
			int barHeight = (int) Math.round((double) bins[i] / max * (chartHeight - 2));
			if (bins[i] > 0 && barHeight == 0)
			{
				barHeight = 1;
			}
			int x = (int) Math.round(i * barWidth);
			int w = Math.max(1, (int) Math.round((i + 1) * barWidth) - x - 1);
			g.setColor(isChatterBin(i) ? CHATTER_COLOR : NORMAL_COLOR);
			g.fillRect(x, chartHeight - barHeight, w, barHeight);
		}

		g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		FontMetrics metrics = g.getFontMetrics();
		int baseline = getHeight() - 3;
		g.drawString("0", 1, baseline);
		String middle = (ButtonStats.BINS - 1) * ButtonStats.BIN_WIDTH_MS / 2 + "";
		g.drawString(middle, (getWidth() - metrics.stringWidth(middle)) / 2, baseline);
		String end = (ButtonStats.BINS - 1) * ButtonStats.BIN_WIDTH_MS + "+ ms";
		g.drawString(end, getWidth() - metrics.stringWidth(end) - 1, baseline);
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		int i = Math.min(bins.length - 1, Math.max(0, event.getX() * bins.length / Math.max(1, getWidth())));
		int from = i * ButtonStats.BIN_WIDTH_MS;
		String range = i == bins.length - 1 ? from + "+ ms" : from + "-" + (from + ButtonStats.BIN_WIDTH_MS - 1) + " ms";
		return range + ": " + bins[i] + " presses";
	}

	private boolean isChatterBin(int i)
	{
		return i * ButtonStats.BIN_WIDTH_MS < thresholdMs;
	}
}
