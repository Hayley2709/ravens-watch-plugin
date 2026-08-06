package com.ravenswatch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RavensWatchPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RavensWatchPlugin.class);
		RuneLite.main(args);
	}
}
