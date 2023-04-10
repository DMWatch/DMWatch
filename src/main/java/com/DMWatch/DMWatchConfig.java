package com.DMWatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(DMWatchConfig.CONFIG_GROUP)
public interface DMWatchConfig extends Config
{
	String CONFIG_GROUP = "DMWatch";
	String PLAYER_OPTION = "playerOption";
	String PLAYER_TEXT_COLOR = "playerTextColor";
	@ConfigSection(
		name = "Overlay Stuff",
		description = "All the options for Overlays behavior",
		position = 30
	)
	String OVERLAY_SECTION = "Overlay";
	@ConfigSection(
		name = "Menu",
		description = "All the options for menu behavior",
		position = 99
	)
	String MENU_SECTION = "Menu";
	@ConfigSection(
		name = "Notifications",
		description = "All the notification options",
		position = 70
	)
	String NOTIFICATIONS_SECTION = "Notifications";

	@ConfigItem(
		position = 3,
		keyName = PLAYER_OPTION,
		name = "Player option",
		description = "Add Challenge in DM option to players",
		section = MENU_SECTION
	)
	default boolean playerOption()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "menuOption",
		name = "Menu option",
		description = "Show Challenge in DM option in menus",
		section = MENU_SECTION
	)
	default boolean menuOption()
	{
		return true;
	}

	@ConfigItem(
		position = 100,
		keyName = "drawOnSelf",
		name = "Draw own color",
		description = "Show own color code",
		section = OVERLAY_SECTION
	)
	default boolean drawOnSelf()
	{
		return true;
	}

	@ConfigItem(
		position = 101,
		keyName = "playerNamePosition",
		name = "Name position",
		description = "Configures the position of drawn player names, or if they should be disabled",
		section = OVERLAY_SECTION
	)
	default PlayerNameLocation playerNamePosition()
	{
		return PlayerNameLocation.ABOVE_HEAD;
	}

	@ConfigItem(
		position = 102,
		keyName = "drawPlayerTiles",
		name = "Draw tiles under players",
		description = "Configures whether or not tiles under highlighted players should be drawn",
		section = OVERLAY_SECTION
	)
	default boolean drawTiles()
	{
		return false;
	}

	@ConfigItem(
		position = 8,
		keyName = "recolorRSNonBanner",
		name = "Recolor RSN on Party Panel",
		description = "Shows the color of a rank on the party panel"
	)
	default boolean recolorRSNonBanner()
	{
		return true;
	}

	@ConfigItem(
		position = 9,
		keyName = "showLogsButton",
		name = "Show Logs button",
		description = "Shows a logs button on side panel"
	)
	default boolean showLogsButton()
	{
		return true;
	}

	@ConfigItem(
		position = 10,
		keyName = "hideIDS",
		name = "Hide ID info",
		description = "Hide the IDs on the Party Panel"
	)
	default boolean hideIDS()
	{
		return true;
	}

	@ConfigItem(
		position = 11,
		keyName = "hideMyWorld",
		name = "Hide my world",
		description = "Don't share world"
	)
	default boolean hideWorld()
	{
		return false;
	}

	@ConfigItem(
		position = 8,
		keyName = "notifyOnJoin",
		name = "Alert On Join",
		description = "Send an alert message when a player on the watchlist enters a Clan/Friends Chat",
		section = NOTIFICATIONS_SECTION
	)
	default boolean notifyOnJoin()
	{
		return true;
	}

	@ConfigItem(
		position = 9,
		keyName = "notifyOnNearby",
		name = "Alert On Nearby",
		description = "Send an alert message when you're nearby a player on the watch list",
		section = NOTIFICATIONS_SECTION
	)
	default boolean notifyOnNearby()
	{
		return true;
	}
}

