package com.DMWatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Singleton
public class PartyMemberTierOverlay extends Overlay
{
	private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 40;
	private static final int ACTOR_HORIZONTAL_TEXT_MARGIN = 10;

	private final PartyMemberIndicatorService playerIndicatorsService;
	private final DMWatchConfig config;
	private final ChatIconManager chatIconManager;
	private final DMWatchPlugin plugin;
	private static final BufferedImage SCAMMER_ICON = ImageUtil.loadImageResource(DMWatchPlugin.class, "scammer.png");

	@Inject
	private PartyMemberTierOverlay(DMWatchConfig config, PartyMemberIndicatorService playerIndicatorsService,
								   ChatIconManager chatIconManager, DMWatchPlugin plugin)
	{
		this.plugin = plugin;
		this.config = config;
		this.playerIndicatorsService = playerIndicatorsService;
		this.chatIconManager = chatIconManager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		playerIndicatorsService.forEachPlayer((player, color) -> renderPlayerOverlay(graphics, player, color));
		return null;
	}

	private void renderPlayerOverlay(Graphics2D graphics, Player actor, Color color)
	{
		final PlayerNameLocation drawPlayerNamesConfig = config.playerNamePosition();
		if (drawPlayerNamesConfig == PlayerNameLocation.DISABLED)
		{
			return;
		}

		final int zOffset;
		switch (drawPlayerNamesConfig)
		{
			case MODEL_CENTER:
			case MODEL_RIGHT:
				zOffset = actor.getLogicalHeight() / 2;
				break;
			default:
				zOffset = actor.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;
		}

		final String name = Text.sanitize(actor.getName());
		Point textLocation = actor.getCanvasTextLocation(graphics, name, zOffset);

		if (drawPlayerNamesConfig == PlayerNameLocation.MODEL_RIGHT)
		{
			textLocation = actor.getCanvasTextLocation(graphics, "", zOffset);

			if (textLocation == null)
			{
				return;
			}

			textLocation = new Point(textLocation.getX() + ACTOR_HORIZONTAL_TEXT_MARGIN, textLocation.getY());
		}

		if (textLocation == null)
		{
			return;
		}

		BufferedImage rankImage = null;
		if (actor.isFriendsChatMember())
		{
			final FriendsChatRank rank = playerIndicatorsService.getFriendsChatRank(actor);

			if (rank != FriendsChatRank.UNRANKED)
			{
				rankImage = chatIconManager.getRankImage(rank);
			}
		}
		else if (actor.isClanMember())
		{
			ClanTitle clanTitle = playerIndicatorsService.getClanTitle(actor);
			if (clanTitle != null)
			{
				rankImage = chatIconManager.getRankImage(clanTitle);
			}
		}

		boolean useScammerIcon = false;
		for (int i = 0; i < plugin.getLocalList().size() && !useScammerIcon; i++)
		{
			Case c = plugin.getLocalList().get(i);

			if (actor.getName().equals(c.getRsn()))
			{
				rankImage = ImageUtil.resizeImage(SCAMMER_ICON, 11, 11);
				useScammerIcon = true;
			}
		}

		if (rankImage != null)
		{
			final int imageWidth = rankImage.getWidth();
			final int imageTextMargin;
			final int imageNegativeMargin;

			if (drawPlayerNamesConfig == PlayerNameLocation.MODEL_RIGHT)
			{
				imageTextMargin = imageWidth;
				imageNegativeMargin = 0;
			}
			else
			{
				imageTextMargin = imageWidth / 2;
				imageNegativeMargin = imageWidth / 2;
			}

			final int textHeight = graphics.getFontMetrics().getHeight() - graphics.getFontMetrics().getMaxDescent();

			if (useScammerIcon) {
				final Point imageLocation = new Point(textLocation.getX() - imageNegativeMargin - 8, textLocation.getY() - textHeight / 2 - rankImage.getHeight() / 2);
				OverlayUtil.renderImageLocation(graphics, imageLocation, rankImage);
			} else {
				final Point imageLocation = new Point(textLocation.getX() - imageNegativeMargin - 1, textLocation.getY() - textHeight / 2 - rankImage.getHeight() / 2);
				OverlayUtil.renderImageLocation(graphics, imageLocation, rankImage);
			}

			if (useScammerIcon) {
				textLocation = new Point(textLocation.getX() - 5 + imageTextMargin, textLocation.getY());
			} else {
				textLocation = new Point(textLocation.getX() + imageTextMargin, textLocation.getY());
			}
		}

		OverlayUtil.renderTextLocation(graphics, textLocation, name, color);
	}
}
