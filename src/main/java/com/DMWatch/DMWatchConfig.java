package com.DMWatch;

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
	String LIVE_OPTION = "makeWatchListLive";
	String DRAWSELF_OPTION = "drawOnSelf";
	String DISCORD_NOTIFY = "discordNotify";

	@ConfigSection(
		name = "Watchlist",
		description = "All the options for Watchlist behavior",
		position = 10
	)
	String WATCHLIST_SECION = "Watch List Endpoint";

	@ConfigSection(
		name = "Side Panel Settings",
		description = "All the options for Side panel",
		position = 20
	)
	String SIDEPANEL_SECTION = "Side Panel";

	@ConfigSection(
		name = "Overlay Settings",
		description = "All the options for Overlays behavior",
		position = 30
	)
	String OVERLAY_SECTION = "Overlay";

	@ConfigSection(
		name = "Notification Settings",
		description = "All the options for notification behavior",
		position = 70
	)
	String NOTIFICATIONS_SECTION = "Notifications";

	@ConfigSection(
		name = "Menu Settings",
		description = "All the options for Menu behavior",
		position = 80
	)
	String MENU_SECTION = "Menu";

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
		keyName = MENU_OPTION,
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
		keyName = DRAWSELF_OPTION,
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
		description = "Shows the color of a rank on the party panel",
		section = SIDEPANEL_SECTION
	)
	default boolean recolorRSNonBanner()
	{
		return true;
	}

	@ConfigItem(
		position = 9,
		keyName = "showLogsButton",
		name = "Show Logs button",
		description = "Shows a logs button on side panel",
		section = SIDEPANEL_SECTION
	)
	default boolean showLogsButton()
	{
		return true;
	}

	@ConfigItem(
		position = 10,
		keyName = "trustAllPlayers",
		name = "Trust players Always",
		description = "Default to trusting players",
		section = SIDEPANEL_SECTION
	)
	default boolean trustAllPlayers()
	{
		return false;
	}

	@ConfigItem(
		position = 11,
		keyName = "hideMyWorld",
		name = "Hide my world",
		description = "Don't share world",
		section = SIDEPANEL_SECTION
	)
	default boolean hideWorld()
	{
		return false;
	}

	@ConfigItem(
		position = 12,
		keyName = "openNav",
		name = "DMWatch side panel on challenge",
		description = "Opens DMWatch side panel when a challenge is issued",
		section = SIDEPANEL_SECTION
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

	@ConfigItem(
		position = 12,
		keyName = LIVE_OPTION,
		name = "Fast List",
		description = "Fast Watchlist",
		warning = "This will make a connection with the DMWatch list to pull live data, a website not controlled or maintained by Jagex or RuneLite",
		section = WATCHLIST_SECION
	)
	default boolean makeWatchListLive()
	{
		return false;
	}

	@Range(min = 5, max = 60)
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
		return 20;
	}
}