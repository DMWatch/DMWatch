package com.DMWatch;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Singleton
public class PartyMemberIndicatorService
{

	private final Client client;
	private final DMWatchPlugin plugin;

	private static final BufferedImage SCAMMER_ICON = ImageUtil.resizeImage(ImageUtil.loadImageResource(DMWatchPlugin.class, "scammer.png"), 11, 11);

	final static HashMap<String, Color> COLORHM;

	static
	{
		COLORHM = new HashMap<>();
		COLORHM.put("0", new Color(234, 123, 91));
		COLORHM.put("1", new Color(252, 242, 4));
		COLORHM.put("9", new Color(252, 242, 4));
		COLORHM.put("10", new Color(252, 242, 4));
		COLORHM.put("11", new Color(252, 242, 4));
		COLORHM.put("2", Color.RED);
		COLORHM.put("3", Color.RED);
		COLORHM.put("4", new Color(188, 84, 4));
		COLORHM.put("5", new Color(236, 236, 220));
		COLORHM.put("6", new Color(104, 68, 164));
		COLORHM.put("7", new Color(84, 252, 28));
		COLORHM.put("8", new Color(244, 204, 64));
	}

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
		ConcurrentHashMap<String, HashSet<String>> mappings = plugin.getMappings();
		if (player.getName() == null)
		{
			return null;
		}

		Color color = null;
		BufferedImage rankImage = null;

		for (String key : mappings.keySet())
		{
			if (mappings.get(key).contains(Text.toJagexName(player.getName().toLowerCase())))
			{
				if (COLORHM.containsKey(key))
				{
					color = COLORHM.get(key);
				}
				if (key.equals("3"))
				{
					rankImage = SCAMMER_ICON;

				}
				return new Decorations(color, rankImage);
			}
		}
		return null;
	}

	@Value
	static class Decorations
	{
		Color color;
		BufferedImage scammerIcon;
	}
}
