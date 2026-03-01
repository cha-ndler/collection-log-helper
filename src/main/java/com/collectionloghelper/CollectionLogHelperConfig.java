package com.collectionloghelper;

import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("collectionloghelper")
public interface CollectionLogHelperConfig extends Config
{
	@ConfigItem(
		keyName = "defaultMode",
		name = "Default Mode",
		description = "The default mode to show when opening the panel"
	)
	default CollectionLogHelperPanel.Mode defaultMode()
	{
		return CollectionLogHelperPanel.Mode.EFFICIENT;
	}

	@ConfigItem(
		keyName = "showOverlays",
		name = "Show Overlays",
		description = "Show guidance overlays when using Guide Me"
	)
	default boolean showOverlays()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayColor",
		name = "Overlay Color",
		description = "Color used for guidance overlays"
	)
	default Color overlayColor()
	{
		return new Color(0, 255, 255);
	}
}
