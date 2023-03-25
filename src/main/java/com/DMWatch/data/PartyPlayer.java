package com.DMWatch.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;

@Data
@EqualsAndHashCode
public class PartyPlayer
{
	private transient PartyMember member;
	private String username;
	private int combatLevel;

	private int world;
	private GameItem[] equipment;
	private GameItem[] inventory;

	private int isVenged;

	private String HWID;
	private String userUnique;
	private String pluginEnabled;
	private String status;


	public PartyPlayer(final PartyMember member)
	{
		this.member = member;
		this.username = "";
		this.inventory = new GameItem[28];
		this.equipment = new GameItem[EquipmentInventorySlot.AMMO.getSlotIdx() + 1];
		this.isVenged = 0;
		this.pluginEnabled = "No";
		this.combatLevel = -1;
		this.world = 0;
		this.status = "0";
		this.HWID = "Unknown";
		this.userUnique = "";
	}

	public PartyPlayer(final PartyMember member, final Client client, final ItemManager itemManager)
	{
		this(member);
		this.isVenged = client.getVarbitValue(Varbits.VENGEANCE_ACTIVE);
		this.pluginEnabled = "No";
		this.world = client.getWorld();
		updatePlayerInfo(client, itemManager);
	}

	public void updatePlayerInfo(final Client client, final ItemManager itemManager)
	{
		// Player is logged in
		if (client.getLocalPlayer() != null)
		{
			this.username = client.getLocalPlayer().getName();

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

			this.combatLevel = client.getLocalPlayer().getCombatLevel();
		}
	}
}
