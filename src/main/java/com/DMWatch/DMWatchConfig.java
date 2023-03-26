package com.DMWatch;

import java.awt.Color;
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
		position = 5,
		keyName = "useHotkey",
		name = "Require Shift-Click",
		description = "Require Shift-Right-Click to view Challenge in DM option in menus",
		section = MENU_SECTION
	)
	default boolean useHotkey()
	{
		return false;
	}

	@ConfigItem(
		position = 6,
		keyName = PLAYER_TEXT_COLOR,
		name = "Highlight color",
		description = "Allows you to change the color of the reported player's rsn in most player lists"
	)
	default Color playerTextColor()
	{
		return new Color(255, 77, 0);
	}

	@ConfigItem(
		position = 7,
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
		position = 8,
		keyName = "notifyOnNearby",
		name = "Alert On Nearby",
		description = "Send an alert message when you're nearby a player on the watch list",
		section = NOTIFICATIONS_SECTION
	)
	default boolean notifyOnNearby()
	{
		return true;
	}

	@ConfigItem(
		position = 9,
		keyName = "hideIDS",
		name = "Hide ID info",
		description = "Hide the IDs on the Party Panel"
	)
	default boolean hideIDS()
	{
		return true;
	}
}
