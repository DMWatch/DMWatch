package com.DMWatch;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(DMWatchConfig.CONFIG_GROUP)
public interface DMWatchConfig extends Config
{
	String CONFIG_GROUP = "DMWatch";
	String PLAYER_OPTION = "playerOption";
	String MENU_OPTION = "menuOption";
	String PLAYER_TEXT_COLOR = "playerTextColor";

	@ConfigSection(
		name = "Overlay Stuff",
		description = "All the options for Overlays behavior",
		position = 30
	)
	String OVERLAY_SECTION = "Overlay";

	@ConfigSection(
		name = "Notifications",
		description = "All the notification options",
		position = 70
	)
	String NOTIFICATIONS_SECTION = "Notifications";

	@ConfigSection(
		name = "Menu",
		description = "All the options for menu behavior",
		position = 80
	)
	String MENU_SECTION = "Menu";

	@ConfigSection(
		name = "Watchlist",
		description = "All the Watchlist stuff",
		position = 90
	)
	String WATCHLIST_SECION = "Watch List Endpoint";

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
		description = "Configures whether or not tiles under highlighted players should be drawn, this can cause lag",
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
		position = 12,
		keyName = "openNav",
		name = "DMWatch side panel on challenge",
		description = "Opens DMWatch side panel when a challenge is issued"
	)
	default boolean openDMWatchSidePanelOnChallenge()
	{
		return true;
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

	@Range(min = -1, max = 30)
	@ConfigItem(
		position = 10,
		keyName = "notifyReminder",
		name = "Same name notify timeout",
		description = "Minutes to wait to notify on same player again, -1 to do it only once",
		section = NOTIFICATIONS_SECTION
	)
	default int notifyReminder()
	{
		return 3;
	}

	@ConfigItem(
		position = 11,
		keyName = "discordNotify",
		name = "Discord Reminder",
		description = "Reminder to join DMWatch discord",
		section = NOTIFICATIONS_SECTION
	)
	default boolean discordNotify()
	{
		return true;
	}

	@Range(min = 15, max = 180)
	@Units(Units.SECONDS)
	@ConfigItem(
		position = 13,
		keyName = "syncLists",
		name = "Sync interval",
		description = "How often to sync data with the list in seconds",
		section = WATCHLIST_SECION
	)
	default int syncLists()
	{
		return 30;
	}
}