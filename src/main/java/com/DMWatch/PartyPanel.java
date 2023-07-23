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
package com.DMWatch;

import com.DMWatch.data.PartyPlayer;
import com.DMWatch.ui.ControlsPanel;
import com.DMWatch.ui.PlayerPanel;
import com.google.inject.Inject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

class PartyPanel extends PluginPanel
{
	private final DMWatchPlugin plugin;

	private final DMWatchConfig config;

	@Getter
	private final HashMap<Long, PlayerPanel> playerPanelMap = new HashMap<>();
	private final JPanel basePanel = new JPanel();
	private final JLabel passphraseLabel = new JLabel();

	@Getter
	private final ControlsPanel controlsPanel;

	@Inject
	PartyPanel(final DMWatchPlugin plugin, DMWatchConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;
		this.setLayout(new BorderLayout());

		basePanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		basePanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));

		final JPanel topPanel = new JPanel();
		topPanel.setBorder(new EmptyBorder(BORDER_OFFSET, 2, BORDER_OFFSET, 2));
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

		JPanel passphrasePanel = new JPanel();
		passphrasePanel.setBorder(new EmptyBorder(4, 0, 0, 0));
		passphrasePanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));

		final JLabel passphraseTopLabel = new JLabel("Party Passphrase");
		passphraseTopLabel.setForeground(Color.WHITE);
		passphraseTopLabel.setHorizontalTextPosition(JLabel.CENTER);
		passphraseTopLabel.setHorizontalAlignment(JLabel.CENTER);

		final JMenuItem copyOpt = new JMenuItem("Copy Passphrase");
		copyOpt.addActionListener(e ->
		{
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(passphraseLabel.getText()), null);
		});

		final JPopupMenu copyPopup = new JPopupMenu();
		copyPopup.setBorder(new EmptyBorder(5, 5, 5, 5));
		copyPopup.add(copyOpt);

		passphraseLabel.setText(plugin.getPartyPassphrase());
		passphraseLabel.setHorizontalTextPosition(JLabel.CENTER);
		passphraseLabel.setHorizontalAlignment(JLabel.CENTER);
		passphraseLabel.setComponentPopupMenu(copyPopup);

		passphrasePanel.add(passphraseTopLabel);
		passphrasePanel.add(passphraseLabel);

		controlsPanel = new ControlsPanel(plugin, config);
		topPanel.add(controlsPanel);
		topPanel.add(passphrasePanel);

		this.add(topPanel, BorderLayout.NORTH);

		// Wrap content to anchor to top and prevent expansion
		final JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(basePanel, BorderLayout.NORTH);
		final JScrollPane scrollPane = new JScrollPane(northPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		this.add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Shows all members of the party, excluding the local player. See {@link com.DMWatch.ui.PlayerBanner )
	 */
	void renderSidebar()
	{
		basePanel.removeAll();

		// Sort by their RSN first; If it doesn't exist sort by their Discord name instead
		final List<PartyPlayer> players = plugin.getPartyMembers().values()
			.stream()
			.sorted(Comparator.comparing(o -> orderByTier(o.getTier(), o.getWorld())))
			.collect(Collectors.toList());

		for (final PartyPlayer player : players)
		{
			drawPlayerPanel(player);
		}

		if (getComponentCount() == 0)
		{
			basePanel.add(new JLabel("There are no members in your party"));
		}

		basePanel.revalidate();
		basePanel.repaint();
	}

	private int orderByTier(String tier, int world)
	{
		if (world == 0)
		{
			return 1000;
		}
		switch (tier)
		{
			case "DMer":
				return 6; // unknown
			case "Smiley": // smiley
			case "Sergeant": // sergeant
			case "Corporal": // corporal
			case "Recruit": // recruit
				return 5; // order these thee same
			case "Scammer":
				return 100; // scammer
			case "Lieutenant":
				return 3; // lt
			case "Captain":
				return 2; // captain
			case "General":
				return 1; // general
			default: // ?
				return 10000;
		}
	}

	void drawPlayerPanel(PartyPlayer player)
	{
		final PlayerPanel panel = playerPanelMap.computeIfAbsent(player.getMember().getMemberId(),
			(k) -> new PlayerPanel(player, config, plugin.spriteManager, plugin.itemManager, plugin));

		final String playerName = player.getUsername() == null ? "" : player.getUsername();
		panel.updatePlayerData(player, !playerName.equals(panel.getPlayer().getUsername())
			|| !player.getTier().equals(panel.getPlayer().getTier())
			|| player.getIsVenged() != panel.getPlayer().getIsVenged()
			|| !player.getUserUnique().equals(panel.getPlayer().getUserUnique())
			|| !player.getReason().equals(panel.getPlayer().getReason())
		);

		basePanel.add(panel);
		basePanel.revalidate();
		basePanel.repaint();
	}

	void removePartyPlayer(final PartyPlayer player)
	{
		if (player != null)
		{
			playerPanelMap.remove(player.getMember().getMemberId());
			renderSidebar();
		}
	}

	public void syncPartyPassphraseVisibility()
	{
		passphraseLabel.setText(plugin.getPartyPassphrase());
	}

	public void updateParty()
	{
		syncPartyPassphraseVisibility();
		controlsPanel.updateControls();
	}
}