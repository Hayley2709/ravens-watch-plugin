package com.ravenswatch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GoogleCalendarPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GoogleCalendarPlugin.class);
		RuneLite.main(args);
	}
}
