package com.collectionloghelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CollectionLogHelperPluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		System.setProperty("collectionlog.devmode", "true");
		ExternalPluginManager.loadBuiltin(CollectionLogHelperPlugin.class);
		RuneLite.main(args);
	}
}
