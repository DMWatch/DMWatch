/*
 * Copyright (c) 2022, TheStonedTurtle <https://github.com/TheStonedTurtle>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.DMWatch.data.events;

import com.DMWatch.data.GameItem;
import com.DMWatch.data.PartyPlayer;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.messages.PartyMemberMessage;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DMPartyBatchedChange extends PartyMemberMessage
{
	int[] i; // Inventory
	int[] e; // equipment
	Collection<DMPartyMiscChange> m = new ArrayList<>(); // Misc Changes
	Collection<DMPartyStatChange> s = new ArrayList<>(); // Stat Changes

	public boolean isValid()
	{
		return i != null
			|| e != null
			|| (s != null && s.size() > 0)
			|| (m != null && m.size() > 0);
	}

	// Unset unneeded variables to minimize payload
	public void removeDefaults()
	{
		s = (s == null || s.size() == 0) ? null : s;
		m = (m == null || m.size() == 0) ? null : m;
	}

	public void process(PartyPlayer player, ItemManager itemManager)
	{
		if (i != null)
		{
			final GameItem[] gameItems = GameItem.convertItemsToGameItems(i, itemManager);
			player.setInventory(gameItems);
		}

		if (s != null)
		{
			s.forEach(change -> change.process(player));
		}

		if (e != null)
		{
			final GameItem[] gameItems = GameItem.convertItemsToGameItems(e, itemManager);
			player.setEquipment(gameItems);
		}

		if (m != null)
		{
			m.forEach(change -> change.process(player));
		}
	}

	public boolean hasBreakingBannerChange()
	{
		return m != null && m.stream().anyMatch(e ->
			e.getT() == DMPartyMiscChange.PartyMisc.V
				|| e.getT() == DMPartyMiscChange.PartyMisc.W
				|| e.getT() == DMPartyMiscChange.PartyMisc.C
				|| e.getT() == DMPartyMiscChange.PartyMisc.HWID
				|| e.getT() == DMPartyMiscChange.PartyMisc.ACCOUNT_HASH
				|| e.getT() == DMPartyMiscChange.PartyMisc.REASON
				|| e.getT() == DMPartyMiscChange.PartyMisc.TIER
		);
	}

	public boolean hasStatChange()
	{
		return s != null && m.stream().anyMatch(e ->
			e.getT() == DMPartyMiscChange.PartyMisc.C
		);
	}
}
