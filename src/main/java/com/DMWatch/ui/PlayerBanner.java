/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
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
package com.DMWatch.ui;

import com.DMWatch.DMWatchConfig;
import com.DMWatch.data.PartyPlayer;
import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Constants;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class PlayerBanner extends JPanel
{
	private static final Dimension STAT_ICON_SIZE = new Dimension(18, 18);
	private static final Dimension ICON_SIZE = new Dimension(Constants.ITEM_SPRITE_WIDTH - 6, Constants.ITEM_SPRITE_HEIGHT - 4);
	private static final BufferedImage EXPAND_ICON = ImageUtil.loadImageResource(PlayerPanel.class, "expand.png");

	@Getter
	private final JPanel statsPanel = new JPanel();
	@Getter
	private final JPanel infoPanel = new JPanel();
	private final Map<String, JLabel> statLabels = new HashMap<>();
	private final Map<String, JLabel> iconLabels = new HashMap<>();
	@Getter
	private final JLabel expandIcon = new JLabel();
	private final JLabel worldLabel = new JLabel();
	private final JLabel iconLabel = new JLabel();

	private final ImageIcon expandIconUp;
	private final ImageIcon expandIconDown;

	@Setter
	@Getter
	private PartyPlayer player;
	private boolean checkIcon;

	private BufferedImage currentVenged = null;

	public PlayerBanner(final PartyPlayer player, boolean expanded, boolean displayWorld, SpriteManager spriteManager, DMWatchConfig config)
	{
		super();
		this.player = player;
		this.setLayout(new GridBagLayout());
		this.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 14, 100));
		this.setBorder(new EmptyBorder(5, 5, 0, 5));

		statsPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 60));
		statsPanel.setLayout(new GridLayout(1, 2));
		statsPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
		statsPanel.setOpaque(true);

		infoPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 60));
		infoPanel.setLayout(new GridLayout(2, 1));
		infoPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
		infoPanel.setOpaque(true);

		expandIconDown = new ImageIcon(EXPAND_ICON);
		expandIconUp = new ImageIcon(ImageUtil.rotateImage(EXPAND_ICON, Math.PI));
		if (expanded)
		{
			expandIcon.setIcon(expandIconUp);
		}
		else
		{
			expandIcon.setIcon(expandIconDown);
		}

		worldLabel.setHorizontalTextPosition(JLabel.LEFT);
		worldLabel.setVisible(displayWorld);

		statsPanel.add(createIconPanel(spriteManager, SpriteID.SPELL_VENGEANCE_OTHER, "IsVenged", player.getIsVenged() == 1 ? "Is Venged" : "Not Venged", false));
		statsPanel.add(createIconPanel(spriteManager, SpriteID.PLAYER_KILLER_SKULL, "DMWatchStatus", msg(player.getStatus()), true));

		infoPanel.add(createTextPanel("pchash", "HWID: " + player.getHWID()));
		infoPanel.add(createTextPanel("acchash", "RID: " + player.getUserUnique()));

		recreatePanel(config.hideIDS());
	}

	private String msg(String status)
	{
		if (status.equals("0"))
		{
			return "User";
		}
		if (status.equals("1"))
		{
			return "Registered";
		}
		if (status.equals("2"))
		{
			return "Accused";
		}
		if (status.equals("3"))
		{
			return "Scammer";
		}
		if (status.equals("4"))
		{
			return "Trusted";
		}
		if (status.equals("5"))
		{
			return "Developer";
		}
		if (status.equals("6"))
		{
			return "B Tier";
		}
		if (status.equals("7"))
		{
			return "A Tier";
		}
		if (status.equals("8"))
		{
			return "S Tier";
		}
		return "Unknown";
	}

	// True = arrow up; False = arrow down
	public void setExpandIcon(boolean direction)
	{
		if (direction)
		{
			expandIcon.setIcon(expandIconUp);
		}
		else
		{
			expandIcon.setIcon(expandIconDown);
		}
	}

	public void recreatePanel(boolean includeIDs)
	{
		removeAll();

		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 1.0;
		c.ipady = 4;

		// Add avatar label regardless of if one exists just to have UI matching
		iconLabel.setBorder(new MatteBorder(1, 1, 1, 1, ColorScheme.DARKER_GRAY_HOVER_COLOR));
		iconLabel.setPreferredSize(ICON_SIZE);
		iconLabel.setMinimumSize(ICON_SIZE);
		iconLabel.setOpaque(false);

		checkIcon = player.getMember().getAvatar() == null;
		if (!checkIcon)
		{
			addIcon();
		}

		add(iconLabel, c);
		c.gridx++;

		final JPanel nameContainer = new JPanel(new GridLayout(2, 1));
		nameContainer.setBorder(new EmptyBorder(0, 5, 0, 0));
		nameContainer.setOpaque(false);

		final JLabel usernameLabel = new JLabel();
		usernameLabel.setLayout(new OverlayLayout(usernameLabel));
		usernameLabel.setHorizontalTextPosition(JLabel.LEFT);
		if (Strings.isNullOrEmpty(player.getUsername()))
		{
			usernameLabel.setText("Not logged in");
		}
		else
		{
			final String levelText = player.getCombatLevel() == -1 ? "" : " (level-" + player.getCombatLevel() + ")";
			usernameLabel.setText(player.getUsername() + levelText);
		}

		expandIcon.setAlignmentX(Component.RIGHT_ALIGNMENT);
		usernameLabel.add(expandIcon, BorderLayout.EAST);
		nameContainer.add(usernameLabel);

		worldLabel.setText("Not logged in");
		if (Strings.isNullOrEmpty(player.getUsername()))
		{
			worldLabel.setText("");
		}
		else if (player.getWorld() > 0)
		{
			worldLabel.setText("World " + player.getWorld());
		}
		nameContainer.add(worldLabel);

		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(nameContainer, c);

		refreshStats();
		c.gridy++;
		c.weightx = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		add(statsPanel, c);

		if (!includeIDs)
		{
			c.gridy++;
			c.weightx = 0;
			c.gridx = 0;
			c.gridwidth = 2;
			add(infoPanel, c);
		}
		revalidate();
		repaint();
	}

	private void addIcon()
	{
		final BufferedImage resized = ImageUtil.resizeImage(player.getMember().getAvatar(), Constants.ITEM_SPRITE_WIDTH - 8, Constants.ITEM_SPRITE_HEIGHT - 4);
		iconLabel.setIcon(new ImageIcon(resized));
	}

	public void refreshStats()
	{
		if (checkIcon)
		{
			if (player.getMember().getAvatar() != null)
			{
				addIcon();
				checkIcon = false;
			}
		}

		statLabels.getOrDefault("IsVenged", new JLabel()).setText(player.getIsVenged() == 1 ? "Is Venged" : "Not Venged");
		statLabels.getOrDefault("DMWatchStatus", new JLabel()).setText(msg(player.getStatus()));
		statLabels.getOrDefault("pchash", new JLabel()).setText("HWID: " + player.getHWID());
		statLabels.getOrDefault("acchash", new JLabel()).setText("RID: " + player.getUserUnique());

		statsPanel.revalidate();
		statsPanel.repaint();
		infoPanel.revalidate();
		infoPanel.repaint();
	}

	private JPanel createIconPanel(final SpriteManager spriteManager, final int spriteID, final String name,
								   final String value, boolean includeHoverText)
	{
		final JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(STAT_ICON_SIZE);
		iconLabels.put(name, iconLabel);
		setSpriteIcon(name, spriteID, spriteManager);

		final JLabel textLabel = new JLabel(value);
		textLabel.setHorizontalAlignment(JLabel.CENTER);
		textLabel.setHorizontalTextPosition(JLabel.CENTER);
		statLabels.put(name, textLabel);

		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(iconLabel, BorderLayout.WEST);
		panel.add(textLabel, BorderLayout.CENTER);
		panel.setOpaque(false);

		if (includeHoverText)
		{
			panel.setToolTipText(name);
		}

		return panel;
	}

	private JPanel createTextPanel(final String name, final String value)
	{
		final JLabel textLabel = new JLabel(value);
		textLabel.setHorizontalAlignment(JLabel.CENTER);
		textLabel.setHorizontalTextPosition(JLabel.CENTER);
		statLabels.put(name, textLabel);

		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(textLabel, BorderLayout.WEST);
		panel.setOpaque(false);

		return panel;
	}

	private void setSpriteIcon(String statLabelKey, final int spriteID, final SpriteManager spriteManager)
	{
		final JLabel label = iconLabels.get(statLabelKey);
		spriteManager.getSpriteAsync(spriteID, 0, img ->
			SwingUtilities.invokeLater(() ->
			{
				if (spriteID == SpriteID.SKILL_PRAYER)
				{
					label.setIcon(new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width + 2, STAT_ICON_SIZE.height + 2)));
				}
				else
				{
					label.setIcon(new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height)));
				}
				label.revalidate();
				label.repaint();
			}));
	}

	private void setBufferedIcon(String statLabelKey, final BufferedImage img)
	{
		final JLabel label = iconLabels.get(statLabelKey);
		SwingUtilities.invokeLater(() ->
		{
			label.setIcon(new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height)));
			label.revalidate();
			label.repaint();
		});
	}

	public void setVenged(final BufferedImage img, SpriteManager spriteManager)
	{
		// If the new value is the same then do nothing
		if ((img == null && currentVenged == null) || (img != null && img.equals(currentVenged)))
		{
			return;
		}
		currentVenged = img;
		if (currentVenged == null)
		{
			setSpriteIcon("IsVenged", SpriteID.SPELL_VENGEANCE_OTHER, spriteManager);
		}
		else
		{
			setSpriteIcon("IsVenged", SpriteID.SPELL_VENGEANCE, spriteManager);
		}
		statsPanel.revalidate();
		statsPanel.repaint();
	}

	public void updateWorld(int world)
	{
		worldLabel.setText("World " + world);
	}
}
