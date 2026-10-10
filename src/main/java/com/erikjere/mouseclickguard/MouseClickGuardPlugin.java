package com.erikjere.mouseclickguard;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Mouse Click Guard",
	description = "Detects mouse switch chatter (unintended double clicks) and can discard the phantom second click",
	tags = {"mouse", "click", "double", "debounce", "chatter", "hardware"}
)
public class MouseClickGuardPlugin extends Plugin
{
	static final String STATS_GROUP = "mouseclickguardstats";
	static final String STATS_KEY = "statistics";

	private static final long SAVE_INTERVAL_SECONDS = 60;
	private static final long PANEL_REFRESH_MS = 1_000;

	@Inject
	private Client client;

	@Inject
	private MouseClickGuardConfig config;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	private final ClickStatistics statistics = new ClickStatistics();
	private final ChatWarnings chatWarnings = new ChatWarnings();

	private ClickGuardMouseListener mouseListener;
	private MouseClickGuardPanel panel;
	private NavigationButton navigationButton;
	private ScheduledFuture<?> saveTask;
	private ScheduledFuture<?> panelTask;
	private volatile boolean panelStale;

	@Override
	protected void startUp()
	{
		load();
		seedHealth();

		mouseListener = new ClickGuardMouseListener(
			config::chatterThreshold,
			button -> button == MouseButton.LEFT ? config.preventLeft() : config.preventRight(),
			this::onPress);
		// First in line, so a suppressed press reaches neither the game nor other plugins.
		mouseManager.registerMouseListener(0, mouseListener);

		panel = new MouseClickGuardPanel(this::resetStatistics);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Mouse Click Guard")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		saveTask = executor.scheduleWithFixedDelay(this::saveIfDirty, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
		panelTask = executor.scheduleWithFixedDelay(this::refreshPanelIfStale, PANEL_REFRESH_MS, PANEL_REFRESH_MS, TimeUnit.MILLISECONDS);
		panelStale = true;
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(mouseListener);
		clientToolbar.removeNavigation(navigationButton);
		saveTask.cancel(false);
		panelTask.cancel(false);
		saveIfDirty();
		mouseListener = null;
		panel = null;
		navigationButton = null;
	}

	private void onPress(PressEvent event)
	{
		statistics.record(event, LocalDate.now());
		chatWarnings.onPress(event);
		chatWarnings.onHealth(event.getButton(), statistics.health(event.getButton(), LocalDate.now()));
		panelStale = true;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		boolean enabled = config.chatWarnings();
		for (String message : chatWarnings.poll(System.currentTimeMillis()))
		{
			if (enabled)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (MouseClickGuardConfig.GROUP.equals(event.getGroup()))
		{
			panelStale = true;
		}
	}

	private void resetStatistics()
	{
		statistics.reset();
		chatWarnings.clearPending();
		seedHealth();
		executor.execute(this::saveIfDirty);
		panelStale = true;
	}

	private void seedHealth()
	{
		LocalDate today = LocalDate.now();
		for (MouseButton button : MouseButton.values())
		{
			chatWarnings.onHealth(button, statistics.health(button, today));
		}
	}

	private void refreshPanelIfStale()
	{
		MouseClickGuardPanel target = panel;
		if (!panelStale || target == null)
		{
			return;
		}
		panelStale = false;
		ClickStatistics.Snapshot snapshot = statistics.snapshot(LocalDate.now());
		int threshold = config.chatterThreshold();
		SwingUtilities.invokeLater(() -> target.update(snapshot, threshold));
	}

	private void load()
	{
		String json = configManager.getConfiguration(STATS_GROUP, STATS_KEY);
		if (json == null)
		{
			return;
		}
		try
		{
			ClickStatistics.Persisted persisted = gson.fromJson(json, ClickStatistics.Persisted.class);
			if (persisted != null)
			{
				statistics.restore(persisted);
			}
		}
		catch (JsonParseException | DateTimeParseException e)
		{
			log.warn("Discarding unreadable Mouse Click Guard statistics", e);
		}
	}

	private void saveIfDirty()
	{
		if (statistics.takeDirty())
		{
			configManager.setConfiguration(STATS_GROUP, STATS_KEY, gson.toJson(statistics.toPersisted()));
		}
	}

	@Provides
	MouseClickGuardConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MouseClickGuardConfig.class);
	}
}
