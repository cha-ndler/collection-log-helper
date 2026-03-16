package com.collectionloghelper.overlay;

/**
 * Shared helper for building tooltip strings used by guidance overlays.
 */
final class OverlayTooltipHelper
{
	private OverlayTooltipHelper()
	{
	}

	/**
	 * Builds a tooltip string from a step description and action, using RuneLite's
	 * {@code </br>} line break format. Returns null if both inputs are empty.
	 */
	static String buildTooltip(String stepDescription, String action)
	{
		StringBuilder tip = new StringBuilder();
		if (stepDescription != null && !stepDescription.isEmpty())
		{
			tip.append("Step: ").append(stepDescription);
		}
		if (action != null && !action.isEmpty())
		{
			if (tip.length() > 0)
			{
				tip.append("</br>");
			}
			tip.append("Action: ").append(action);
		}
		return tip.length() > 0 ? tip.toString() : null;
	}
}
