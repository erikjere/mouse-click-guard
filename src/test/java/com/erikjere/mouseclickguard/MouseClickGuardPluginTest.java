package com.erikjere.mouseclickguard;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MouseClickGuardPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MouseClickGuardPlugin.class);
		RuneLite.main(args);
	}
}
