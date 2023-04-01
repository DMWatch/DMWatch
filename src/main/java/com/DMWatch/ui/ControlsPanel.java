/*
 * Copyright (c) 2021, Jonathan Rousseau <https://github.com/JoRouss>
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
package com.DMWatch.ui;

import com.DMWatch.DMWatchPlugin;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

// A copy of the controls from the `net.runelite.client.plugins.party.PartyPanel` class
public class ControlsPanel extends JPanel
{
	private static final BufferedImage DISCORD_IMG = ImageUtil.loadImageResource(DMWatchPlugin.class, "discord-mark-blue.png");
	private static ImageIcon DISCORD_ICON;
	private static ImageIcon DISCORD_HOVER_ICON;
	private final JButton joinPartyButton = new JButton();
	private final JLabel discordTextLabel = new JLabel();
	private final DMWatchPlugin plugin;

	public ControlsPanel(DMWatchPlugin plugin)
	{
		this.plugin = plugin;
		this.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 2, 4, 2);

		c.gridx = 0;
		c.gridy = 0;
		this.add(joinPartyButton, c);

		joinPartyButton.setText("Join DMWatch");
		joinPartyButton.setFocusable(false);
		joinPartyButton.addActionListener(e ->
		{
			if (!plugin.isInParty() || !plugin.getPartyPassphrase().equals("DMW"))
			{
				plugin.changeParty("DMW");
			}
			else
			{
				plugin.leaveParty();
			}
		});

		c.gridx = 1;
		c.gridy = 0;
		this.add(discordTextLabel, c);
		discordTextLabel.setText("Join the Discord");

		c.gridx = 2;
		c.gridy = 0;
		DISCORD_ICON = new ImageIcon(ImageUtil.resizeImage(DISCORD_IMG, 24,18));
		DISCORD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(ImageUtil.resizeImage(DISCORD_IMG, 24,18), 0.53f));
		JLabel joinDiscordLabel = new JLabel(DISCORD_ICON);

		this.add(joinDiscordLabel, c);
		joinDiscordLabel.setToolTipText("Click to join our discord");
		joinDiscordLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					LinkBrowser.browse("https://discord.gg/dm");
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				joinDiscordLabel.setIcon(DISCORD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				joinDiscordLabel.setIcon(DISCORD_ICON);
			}
		});

		updateControls();
	}

	public void updateControls()
	{
		joinPartyButton.setText(plugin.isInParty() && plugin.getPartyPassphrase().equals("DMW") ? "Leave DMW" : "Join DMW");
		joinPartyButton.revalidate();
	}
}
