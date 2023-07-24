package com.DMWatch;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import static net.runelite.api.FriendsChatRank.UNRANKED;
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
	private final DMWatchPlugin plugin;

	@Inject
	private PartyMemberIndicatorService(Client client, DMWatchPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
	}

	public void forEachPlayer(final BiConsumer<Player, Decorations> consumer)
	{
		for (Player player : client.getPlayers())
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}

			Decorations decorations = getDecorations(player);
			if (decorations != null && decorations.getColor() != null)
			{
				consumer.accept(player, decorations);
			}
		}
	}

	Decorations getDecorations(Player player)
	{
		if (client.getLocalPlayer() == player && !plugin.isRenderOnSelf())
		{
			return null;
		}

		HashSet<String> localList = plugin.getLocalScammers();
		if (player.getName() == null)
		{
			return null;
		}

		Color color = null;
		BufferedImage dmwatchIcon = null;
		FriendsChatRank rank = null;
		ClanTitle clanTitle = null;


		String rsn = Text.toJagexName(player.getName().toLowerCase());

		if (localList.size() != 0 && localList.contains(rsn))
		{
			String key = "Scammer";
			color = DMWatchUtil.COLORHM.get(key);
			dmwatchIcon = DMWatchUtil.getRankIconOverlay(key);

			rank = getFriendsChatRank(player);

			if (player.isClanMember() && plugin.isShowClanRanks())
			{
				clanTitle = getClanTitle(player);
			}

			return new Decorations(rank, clanTitle, color, dmwatchIcon, player.isFriendsChatMember());
		}

		if (plugin.isScammerRSN(rsn))
		{
			String key = "Scammer";
			color = DMWatchUtil.COLORHM.get(key);
			dmwatchIcon = DMWatchUtil.getRankIconOverlay(key);

			rank = getFriendsChatRank(player);

			if (player.isClanMember() && plugin.isShowClanRanks())
			{
				clanTitle = getClanTitle(player);
			}

			return new Decorations(rank, clanTitle, color, dmwatchIcon, player.isFriendsChatMember());
		}

		ConcurrentHashMap<String, String> rankedMappings = plugin.getRankedMappings();

		if (rankedMappings.containsKey(rsn))
		{
			String key = rankedMappings.get(rsn);
			if (DMWatchUtil.COLORHM.containsKey(key))
			{
				color = DMWatchUtil.COLORHM.get(key);
			}

			dmwatchIcon = DMWatchUtil.getRankIconOverlay(key);
			rank = getFriendsChatRank(player);

			if (player.isClanMember() && plugin.isShowClanRanks())
			{
				clanTitle = getClanTitle(player);
			}

			return new Decorations(rank, clanTitle, color, dmwatchIcon, player.isFriendsChatMember());
		}

		return null;
	}

	@Value
	static class Decorations
	{
		FriendsChatRank friendsChatRank;
		ClanTitle clanTitle;
		Color color;
		BufferedImage dmwatchIcon;
		boolean isFriendsChatMember;
	}

	private ClanTitle getClanTitle(Player player)
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

	private FriendsChatRank getFriendsChatRank(Player player)
	{
		if (player.isFriendsChatMember() && plugin.isShowFriendRanks())
		{
			final FriendsChatManager friendsChatManager = client.getFriendsChatManager();
			if (friendsChatManager == null)
			{
				return UNRANKED;
			}

			FriendsChatMember friendsChatMember = friendsChatManager.findByName(Text.removeTags(player.getName()));
			return friendsChatMember != null ? friendsChatMember.getRank() : UNRANKED;
		}

		RankedPlayer rp = plugin.rankedCaseManager.get(player.getName());

		if (rp == null)
		{
			return null;
		}

		switch (rp.getRank())
		{
			case "Smiley":
				return FriendsChatRank.FRIEND;
			case "Recruit":
				return FriendsChatRank.RECRUIT;
			case "Sergeant":
				return FriendsChatRank.SERGEANT;
			case "Corporal":
				return FriendsChatRank.CORPORAL;
			case "Lieutenant":
				return FriendsChatRank.LIEUTENANT;
			case "Captain":
				return FriendsChatRank.CAPTAIN;
			case "General":
				return FriendsChatRank.GENERAL;
			default:
				return UNRANKED;
		}
	}
}
