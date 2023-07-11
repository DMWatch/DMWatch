package com.DMWatch.data;

import com.DMWatch.DMWatchConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;

@Data
@EqualsAndHashCode
public class PartyPlayer
{
	private transient PartyMember member;
	private String username;
	private Stats stats;
	private GameItem[] equipment;
	private GameItem[] inventory;
	private int isVenged;
	private String HWID;
	private String userUnique;
	private String pluginEnabled;
	private String tier;
	private String reason;
	private int world;

	public PartyPlayer(final PartyMember member)
	{
		this.member = member;
		this.username = "";
		this.stats = null;
		this.inventory = new GameItem[28];
		this.equipment = new GameItem[EquipmentInventorySlot.AMMO.getSlotIdx() + 1];
		this.isVenged = 0;
		this.world = 0;
		this.tier = "DMer";
		this.HWID = "unknown";
		this.userUnique = "";
		this.reason = "";
		this.world = 0;
	}

	public PartyPlayer(final PartyMember member, final Client client, DMWatchConfig config, final ItemManager itemManager)
	{
		this(member);
		this.isVenged = client.getVarbitValue(Varbits.VENGEANCE_ACTIVE);
		this.world = config.hideWorld() ? -1 : client.getWorld();

		updatePlayerInfo(client, itemManager);
	}

	public void updatePlayerInfo(final Client client, final ItemManager itemManager)
	{
		// Player is logged in
		if (client.getLocalPlayer() != null)
		{
			this.username = client.getLocalPlayer().getName();
			this.stats = new Stats(client);

			final ItemContainer invi = client.getItemContainer(InventoryID.INVENTORY);
			if (invi != null)
			{
				this.inventory = GameItem.convertItemsToGameItems(invi.getItems(), itemManager);
			}

			final ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
			if (equip != null)
			{
				this.equipment = GameItem.convertItemsToGameItems(equip.getItems(), itemManager);
			}
		}
	}

	public int getSkillBoostedLevel(final Skill skill)
	{
		if (stats == null)
		{
			return 0;
		}

		return stats.getBoostedLevels().get(skill);
	}

	public int getSkillRealLevel(final Skill skill)
	{
		return getSkillRealLevel(skill, false);
	}

	public int getSkillRealLevel(final Skill skill, final boolean allowVirtualLevels)
	{
		if (stats == null)
		{
			return 0;
		}

		assert skill != Skill.OVERALL;

		return Math.min(stats.getBaseLevels().get(skill), allowVirtualLevels ? 126 : 99);
	}

	public void setSkillsBoostedLevel(final Skill skill, final int level)
	{
		if (stats == null)
		{
			return;
		}

		stats.getBoostedLevels().put(skill, level);
	}

	public void setSkillsRealLevel(final Skill skill, final int level)
	{
		if (stats == null)
		{
			return;
		}

		stats.getBaseLevels().put(skill, level);
	}
}