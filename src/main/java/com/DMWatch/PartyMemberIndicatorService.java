package com.DMWatch;

import java.awt.Color;
import java.util.HashMap;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.util.Text;

@Singleton

public class PartyMemberIndicatorService
{

	private final Client client;
	private final DMWatchConfig config;
	private final DMWatchPlugin plugin;

	final static HashMap<String, Color> COLORHM;

	static
	{
		COLORHM = new HashMap<>();
		COLORHM.put("0", new Color(234, 123, 91));
		COLORHM.put("1", new Color(252, 242, 4));
		COLORHM.put("9", new Color(252, 242, 4));
		COLORHM.put("10", new Color(252, 242, 4));
		COLORHM.put("11", new Color(252, 242, 4));
		COLORHM.put("2", Color.YELLOW);
		COLORHM.put("3", Color.RED);
		COLORHM.put("4", new Color(188, 84, 4));
		COLORHM.put("5", new Color(236, 236, 220));
		COLORHM.put("6", new Color(104, 68, 164));
		COLORHM.put("7", new Color(84, 252, 28));
		COLORHM.put("8", new Color(244, 204, 64));
	}

	@Inject
	private PartyMemberIndicatorService(Client client, DMWatchConfig config, DMWatchPlugin plugin)
	{
		this.config = config;
		this.client = client;
		this.plugin = plugin;
	}

	public void forEachPlayer(final BiConsumer<Player, Color> consumer)
	{

		final Player localPlayer = client.getLocalPlayer();

		for (Player player : client.getPlayers())
		{
			boolean recolored = false;
			if (player == null || player.getName() == null)
			{
				continue;
			}

			for (int i = 0; i < plugin.getLocalList().size() && !recolored; i++)
			{
				Case c = plugin.getLocalList().get(i);
				if (Text.toJagexName(player.getName()).equalsIgnoreCase(Text.toJagexName(c.getRsn())))
				{
					if (COLORHM.containsKey(c.getStatus()))
					{
						consumer.accept(player, COLORHM.get(c.getStatus()));
						recolored = true;
					}
				}
			}

			if (!recolored)
			{
				if (player == localPlayer)
				{
					if (config.drawOnSelf() && plugin.getMyPlayer() != null)
					{
						if (COLORHM.containsKey(plugin.getMyPlayer().getStatus()))
						{
							consumer.accept(player, COLORHM.get(plugin.getMyPlayer().getStatus()));
						}
					}
				}
				else if (plugin.isInParty() && plugin.otherPlayerInParty(player.getName()))
				{
					if (COLORHM.containsKey(plugin.getPlayerTier(player.getName())))
					{
						consumer.accept(player, COLORHM.get(plugin.getPlayerTier(player.getName())));
					}
				}
			}

		}
	}

	ClanTitle getClanTitle(Player player)
	{
		ClanChannel clanChannel = client.getClanChannel();
		ClanSettings clanSettings = client.getClanSettings();
		if (clanChannel == null || clanSettings == null)
		{
			return null;
		}

		ClanChannelMember member = clanChannel.findMember(player.getName());
		if (member == null)
		{
			return null;
		}

		ClanRank rank = member.getRank();
		return clanSettings.titleForRank(rank);
	}

	FriendsChatRank getFriendsChatRank(Player player)
	{
		final FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (friendsChatManager == null)
		{
			return FriendsChatRank.UNRANKED;
		}

		FriendsChatMember friendsChatMember = friendsChatManager.findByName(Text.removeTags(player.getName()));
		return friendsChatMember != null ? friendsChatMember.getRank() : FriendsChatRank.UNRANKED;
	}
}
