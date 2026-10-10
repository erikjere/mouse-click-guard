package com.erikjere.mouseclickguard;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(MouseClickGuardConfig.GROUP)
public interface MouseClickGuardConfig extends Config
{
	String GROUP = "mouseclickguard";

	@ConfigSection(
		name = "Click prevention",
		description = "Discard chatter presses before the game sees them",
		position = 1
	)
	String preventionSection = "prevention";

	@ConfigSection(
		name = "Chat warnings",
		description = "Chat messages about chatter and mouse health",
		position = 2
	)
	String warningsSection = "warnings";

	@Range(min = 10, max = 150)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "chatterThreshold",
		name = "Chatter threshold",
		description = "A press that follows the previous press of the same button sooner than this is counted as chatter."
			+ " Deliberate fast clicking is usually above 80 ms.",
		position = 0
	)
	default int chatterThreshold()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "preventLeft",
		name = "Prevent left chatter",
		description = "Discard a left press that comes sooner than the chatter threshold after the last accepted left press",
		section = preventionSection,
		position = 0
	)
	default boolean preventLeft()
	{
		return false;
	}

	@ConfigItem(
		keyName = "preventRight",
		name = "Prevent right chatter",
		description = "Discard a right press that comes sooner than the chatter threshold after the last accepted right press",
		section = preventionSection,
		position = 1
	)
	default boolean preventRight()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chatWarnings",
		name = "Chat warnings",
		description = "Summarise chatter in chat at most once every 5 minutes per button, and announce mouse health changes",
		section = warningsSection,
		position = 0
	)
	default boolean chatWarnings()
	{
		return true;
	}
}
