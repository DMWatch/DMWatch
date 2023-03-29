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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
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
import net.runelite.client.util.LinkBrowser;

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

	private final BufferedImage currentVenged = null;
	private final BufferedImage isStreamer = null;
	DMWatchConfig config;

	public PlayerBanner(final PartyPlayer player, boolean expanded, boolean displayWorld, SpriteManager spriteManager, DMWatchConfig config)
	{
		super();
		this.config = config;
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

		statsPanel.add(createIconPanel(spriteManager, SpriteID.SPELL_VENGEANCE_OTHER, "IsVenged", player.getIsVenged() == 1 ? "Is Venged" : "Not Venged", "", false));
		statsPanel.add(createIconPanel(spriteManager, SpriteID.PLAYER_KILLER_SKULL, "DMWatchStatus", msg(player.getStatus()), player.getReason(), true));

		infoPanel.add(createTextPanel("pchash", "HWID: " + player.getHWID()));
		infoPanel.add(createTextPanel("acchash", "RID: " + player.getUserUnique()));

		recreatePanel(config.hideIDS());
	}

	private String msg(String status)
	{
		if (status.equals("0"))
		{
			return "Unregistered";
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
			return "Streamer";
		}
		if (status.equals("7"))
		{
			return "Streamer";
		}
		if (status.equals("8"))
		{
			return "Top G";
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
		updateWorld(player.getWorld());
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
		statLabels.get("DMWatchStatus").setToolTipText(player.getReason());
		statLabels.getOrDefault("pchash", new JLabel()).setText("HWID: " + player.getHWID());
		statLabels.getOrDefault("acchash", new JLabel()).setText("RID: " + player.getUserUnique());

		statsPanel.revalidate();
		statsPanel.repaint();
		infoPanel.revalidate();
		infoPanel.repaint();
	}

	private JPanel createIconPanel(final SpriteManager spriteManager, final int spriteID, final String name,
								   final String value, String hoverOverText, boolean includeHoverText)
	{
		final JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(STAT_ICON_SIZE);
		iconLabels.put(name, iconLabel);
		setSpriteIcon(name, spriteID, spriteManager);

		final JLabel textLabel = new JLabel(value);
		textLabel.setHorizontalAlignment(JLabel.LEFT);
		textLabel.setHorizontalTextPosition(JLabel.LEFT);
		statLabels.put(name, textLabel);

		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(iconLabel, BorderLayout.WEST);
		panel.add(textLabel, BorderLayout.CENTER);
		panel.setOpaque(false);

		if (includeHoverText)
		{
			panel.setToolTipText(hoverOverText);
		}

		return panel;
	}

	private JPanel createTextPanel(final String name, final String value)
	{
		final JLabel textLabel = new JLabel(value);
		textLabel.setHorizontalAlignment(JLabel.LEFT);
		textLabel.setHorizontalTextPosition(JLabel.LEFT);
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
		if (!player.getStatus().equals("5") && !player.getStatus().equals("6"))
		{
			SwingUtilities.invokeLater(() ->
			{
				ImageIcon ic = new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height));
				label.setIcon(ic);
				label.revalidate();
				label.repaint();
			});
			return;
		}

		if (player.getStatus().equals("6"))
		{
			ImageIcon ic = new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height));
			ImageIcon hoverIC = new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height));
			label.setIcon(ic);
			SwingUtilities.invokeLater(() ->
			{
				if (label.getMouseListeners().length == 0)
				{
					label.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mousePressed(MouseEvent e)
						{
							if (SwingUtilities.isLeftMouseButton(e))
							{
								LinkBrowser.browse("https://www.twitch.tv/" + player.getReason());
							}
						}

						@Override
						public void mouseEntered(MouseEvent e)
						{
							label.setIcon(hoverIC);
						}

						@Override
						public void mouseExited(MouseEvent e)
						{
							label.setIcon(ic);
						}
					});
					label.revalidate();
				}
			});
		}

		if (player.getStatus().equals("7"))
		{
			ImageIcon ic = new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height));
			ImageIcon hoverIC = new ImageIcon(ImageUtil.resizeImage(img, STAT_ICON_SIZE.width, STAT_ICON_SIZE.height));
			label.setIcon(ic);

			SwingUtilities.invokeLater(() ->
			{
				if (label.getMouseListeners().length == 0)
				{
					label.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mousePressed(MouseEvent e)
						{
							if (SwingUtilities.isLeftMouseButton(e))
							{
								LinkBrowser.browse("https://www.kick.com/" + player.getReason());
							}
						}

						@Override
						public void mouseEntered(MouseEvent e)
						{
							label.setIcon(hoverIC);
						}

						@Override
						public void mouseExited(MouseEvent e)
						{
							label.setIcon(ic);
						}
					});
					label.revalidate();
				}
			});
		}
	}

	public void setVenged(boolean currentVenged, SpriteManager spriteManager)
	{
		// If the new value is the same then do nothing
		if (!currentVenged)
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

	// TODO make this more generic
	public void setStreamerIcon(boolean b, BufferedImage bufferedImage, SpriteManager spriteManager)
	{
		if (b)
		{
			setBufferedIcon("DMWatchStatus", bufferedImage);
		}
		else
		{
			setSpriteIcon("DMWatchStatus", SpriteID.PLAYER_KILLER_SKULL, spriteManager);
		}
		statsPanel.revalidate();
		statsPanel.repaint();
	}

	public void updateWorld(int world)
	{
		if (world == -1)
		{
			worldLabel.setText("Hidden");
		}
		else if (world == 0)
		{
			worldLabel.setText("Logged out");
		}
		else
		{
			worldLabel.setText("World " + world);
		}
	}
}
