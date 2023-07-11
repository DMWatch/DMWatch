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
import com.DMWatch.DMWatchPlugin;
import com.DMWatch.data.GameItem;
import com.DMWatch.data.PartyPlayer;
import com.DMWatch.ui.equipment.EquipmentPanelSlot;
import com.DMWatch.ui.equipment.PlayerEquipmentPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Getter
public class PlayerPanel extends JPanel
{
	private static final Dimension IMAGE_SIZE = new Dimension(24, 24);
	private static final Color BACKGROUND_COLOR = ColorScheme.DARK_GRAY_COLOR;
	private static final Color BACKGROUND_HOVER_COLOR = ColorScheme.DARKER_GRAY_COLOR;
	private static final BufferedImage EXPAND_ICON = ImageUtil.loadImageResource(PlayerPanel.class, "expand.png");

	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final PlayerBanner banner;
	private final PlayerInventoryPanel inventoryPanel;
	private final PlayerEquipmentPanel equipmentPanel;
	private final DMWatchConfig config;
	private final Map<Integer, Boolean> tabMap = new HashMap<>();
	private PartyPlayer player;
	private final DMWatchPlugin plugin;

	@Setter
	private boolean showInfo;

	private boolean JOptionPaneOpened;

	public PlayerPanel(final PartyPlayer selectedPlayer, final DMWatchConfig config,
					   final SpriteManager spriteManager, final ItemManager itemManager, DMWatchPlugin plugin)
	{
		this.plugin = plugin;
		this.player = selectedPlayer;
		this.config = config;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		this.showInfo = false;
		this.banner = new PlayerBanner(selectedPlayer, showInfo, spriteManager, config, plugin);
		this.inventoryPanel = new PlayerInventoryPanel(selectedPlayer.getInventory(), banner.getTrustedPlayerButton().isSelected(), itemManager);
		this.equipmentPanel = new PlayerEquipmentPanel(selectedPlayer.getEquipment(), spriteManager, itemManager);

		// Non-optimal way to attach a mouse listener to
		// the entire panel, but easy to implement
		JPanel statsPanel = this.banner.getStatsPanel();
		JLabel expandIcon = this.banner.getExpandIcon();

		final ImageIcon expandIconUp = banner.getExpandIconUp();
		final ImageIcon expandIconDown = banner.getExpandIconDown();

		final JMenuItem copyOpt = new JMenuItem("Copy IDs");
		copyOpt.addActionListener(e ->
		{
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection("HWID: " + player.getHWID() + "\nRID: " + player.getUserUnique()), null);
		});
		final JPopupMenu copyPopup = new JPopupMenu();
		copyPopup.setBorder(new EmptyBorder(5, 5, 5, 5));
		copyPopup.add(copyOpt);

		banner.setComponentPopupMenu(copyPopup);

		Component[] list = new Component[statsPanel.getComponentCount() + 1];
		System.arraycopy(statsPanel.getComponents(), 0, list, 0, list.length - 1);
		list[list.length - 1] = banner;

		for (Component comp : list)
		{
			if (comp instanceof JPanel)
			{
				comp.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mousePressed(MouseEvent e)
					{
						if (e.getButton() == MouseEvent.BUTTON1)
						{
							showInfo = !showInfo;
							if (showInfo)
							{
								expandIcon.setIcon(expandIconUp);
							}
							else
							{
								expandIcon.setIcon(expandIconDown);
							}
							updatePanel();
						}
					}

					@Override
					public void mouseEntered(MouseEvent e)
					{
						banner.setBackground(BACKGROUND_HOVER_COLOR);
						statsPanel.setBackground(BACKGROUND_HOVER_COLOR);
					}

					@Override
					public void mouseExited(MouseEvent e)
					{
						banner.setBackground(BACKGROUND_COLOR);
						statsPanel.setBackground(BACKGROUND_COLOR);
					}
				});
			}
		}

		// Add event listener here as we want to update their inventory when the state changes and we cant access it from inside PlayerBanner
		banner.getTrustedPlayerButton().addItemListener((i) -> {
			if (i.getStateChange() != ItemEvent.SELECTED) {
				// Ensure we update inventory on deselects
				inventoryPanel.updateInventory(player.getInventory(), banner.getTrustedPlayerButton().isSelected(), JOptionPaneOpened);
				return;
			}

			SwingUtilities.invokeLater(() -> {
				JOptionPaneOpened = true;
				final int confirm = JOptionPane.showConfirmDialog(
					this,
					"<html>Are you sure you want to trust this player?<br/>By clicking this box you are indicating you fully trust this player not to spoof any of their data.",
					"Trust player " + player.getUsername() + "?",
					JOptionPane.YES_NO_OPTION);

				if (confirm == JOptionPane.NO_OPTION || confirm == -1) {
					banner.getTrustedPlayerButton().setSelected(false);
					return;
				}

				// Delay inventory update until they click the confirm the button
				inventoryPanel.updateInventory(player.getInventory(), banner.getTrustedPlayerButton().isSelected(), JOptionPaneOpened);
				JOptionPaneOpened = false;
			});
		});

		updatePanel();

		revalidate();
		repaint();
	}

	private void addTab(final MaterialTabGroup tabGroup, final int spriteID, final JPanel panel, final String tooltip)
	{
		spriteManager.getSpriteAsync(spriteID, 0, img ->
			SwingUtilities.invokeLater(() ->
			{
				final MaterialTab tab = new MaterialTab(createImageIcon(img), tabGroup, panel);
				tab.setToolTipText(tooltip);
				tabGroup.addTab(tab);
				tabGroup.revalidate();
				tabGroup.repaint();

				tabMap.put(spriteID, false);
				tab.setOnSelectEvent(() -> {
					tabMap.replaceAll((k, v) -> v = false);
					tabMap.put(spriteID, true);
					updatePlayerData(player, false);
					return true;
				});

				if (spriteID == SpriteID.TAB_INVENTORY)
				{
					tabGroup.select(tab);
					tabMap.put(spriteID, true);
				}
			}));
	}

	private ImageIcon createImageIcon(BufferedImage image)
	{
		return new ImageIcon(ImageUtil.resizeImage(image, IMAGE_SIZE.width, IMAGE_SIZE.height));
	}

	// TODO add smarter ways to update data
	public void updatePlayerData(PartyPlayer newPlayer, boolean hasBreakingBannerChange)
	{
		player = newPlayer;
		banner.setPlayer(player);

		if (hasBreakingBannerChange)
		{
			banner.recreatePanel();
		}

		if (player.getStats() != null)
		{
			banner.refreshStats();
		}

		banner.setVenged(player.getIsVenged() == 1, spriteManager);
		banner.setStreamerIcon(player.getTier(), spriteManager);

		if (!showInfo)
		{
			return;
		}

		if (tabMap.getOrDefault(SpriteID.TAB_INVENTORY, false))
		{
			inventoryPanel.updateInventory(player.getInventory(), banner.getTrustedPlayerButton().isSelected(), JOptionPaneOpened);
		}

		if (tabMap.getOrDefault(SpriteID.TAB_EQUIPMENT, false))
		{
			for (final EquipmentInventorySlot equipSlot : EquipmentInventorySlot.values())
			{
				GameItem item = null;
				if (player.getEquipment().length > equipSlot.getSlotIdx())
				{
					item = player.getEquipment()[equipSlot.getSlotIdx()];
				}

				final EquipmentPanelSlot slot = this.equipmentPanel.getPanelMap().get(equipSlot);
				if (item != null && slot != null)
				{
					final AsyncBufferedImage img = itemManager.getImage(item.getId(), item.getQty(), item.isStackable());
					slot.setGameItem(item, img);

					// Ensure item is set when image loads
					final GameItem finalItem = item;
					img.onLoaded(() -> slot.setGameItem(finalItem, img));
				}
				else if (slot != null)
				{
					slot.setGameItem(null, null);
				}
			}
		}
	}

	public void updatePanel()
	{
		this.removeAll();
		Color color = new Color(87, 80, 64);
		int thickness = 2;

		if (showInfo)
		{
			this.setBorder(new CompoundBorder(
				new MatteBorder(thickness, thickness, thickness, thickness, color),
				new EmptyBorder(0, 0, 5, 0))
			);
		}
		else
		{
			this.setBorder(new MatteBorder(thickness, thickness, thickness, thickness, color));
		}

		final JPanel view = new JPanel();
		view.setBorder(new EmptyBorder(5, 5, 0, 5));
		final MaterialTabGroup tabGroup = new MaterialTabGroup(view);
		tabGroup.setBorder(new EmptyBorder(10, 0, 4, 0));

		tabMap.clear();
		addTab(tabGroup, SpriteID.TAB_INVENTORY, inventoryPanel, "Inventory");
		addTab(tabGroup, SpriteID.TAB_EQUIPMENT, equipmentPanel, "Equipment");

		setLayout(new DynamicGridLayout(0, 1));

		add(banner);
		if (this.showInfo)
		{
			add(tabGroup);
			add(view);
			banner.getInfoPanel().setVisible(true);

		}
		else
		{
			banner.getInfoPanel().setVisible(false);
		}

		revalidate();
		repaint();
	}
}