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

import com.DMWatch.DMWatchConfig;
import com.DMWatch.DMWatchPlugin;
import static com.DMWatch.DMWatchPlugin.DMWATCH_DIR;
import java.awt.Component;
import java.awt.Container;
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

public class ControlsPanel extends JPanel
{
	private static final BufferedImage DISCORD_IMG = ImageUtil.loadImageResource(DMWatchPlugin.class, "discord-mark-blue.png");
	private static final BufferedImage FRIENDS_CHAT_ICON_IMG = ImageUtil.loadImageResource(DMWatchPlugin.class, "friends-chat-icon.png");
	private static ImageIcon DISCORD_ICON;
	private static ImageIcon DISCORD_HOVER_ICON;
	private final JButton joinPartyButton = new JButton();
	private final JButton openLogs = new JButton();
	private final JLabel discordTextLabel = new JLabel();
	private final DMWatchPlugin plugin;

	private static final Insets insets = new Insets(0, 0, 0, 0);

	public ControlsPanel(DMWatchPlugin plugin, DMWatchConfig config)
	{
		this.plugin = plugin;
		this.setLayout(new GridBagLayout());

		openLogs.setText("Open Logs");
		openLogs.setFocusable(false);
		openLogs.addActionListener(e ->
		{
			LinkBrowser.open(DMWATCH_DIR.toString());
		});

		joinPartyButton.setText("Join DMWatch Party");
		joinPartyButton.setFocusable(false);
		joinPartyButton.addActionListener(e ->
		{
			if (!plugin.isInParty() || !plugin.getPartyPassphrase().equals("DMWatch"))
			{
				plugin.changeParty("DMWatch");
				plugin.setOpponent("");
			}
			else
			{
				plugin.setOpponent("");
				plugin.leaveParty();
			}
		});

		JLabel inGameFC = new JLabel();
		inGameFC.setText("DMWatch");
		inGameFC.setToolTipText("Join DMWatch chat in game!");
		inGameFC.setIcon(new ImageIcon(FRIENDS_CHAT_ICON_IMG));

		DISCORD_ICON = new ImageIcon(ImageUtil.resizeImage(DISCORD_IMG, 24, 18));
		DISCORD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(ImageUtil.resizeImage(DISCORD_IMG, 24, 18), 0.53f));

		discordTextLabel.setIcon(DISCORD_ICON);

		addComponent(this, discordTextLabel, 0, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.WEST);
		addComponent(this, inGameFC, 2, 0, 2, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
		addComponent(this, joinPartyButton, 0, 1, 2, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
		if (config.showLogsButton())
		{
			addComponent(this, openLogs, 3, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
		}
		discordTextLabel.setText("Join the Discord");
		discordTextLabel.setToolTipText("Click to join our discord!");
		discordTextLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					LinkBrowser.browse("https://discord.gg/dmwatch");
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				discordTextLabel.setIcon(DISCORD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				discordTextLabel.setIcon(DISCORD_ICON);
			}
		});

		updateControls();
	}

	public void updateControls()
	{
		joinPartyButton.setText(plugin.isInParty() && plugin.getPartyPassphrase().equals("DMWatch") ? "Leave DMWatch Party" : "Join DMWatch Party");
		joinPartyButton.revalidate();
	}

	private void addComponent(Container container, Component component, int gridx, int gridy,
							  int gridwidth, int gridheight, int anchor, int fill)
	{
		GridBagConstraints gbc = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 1.0, 1.0,
			anchor, fill, insets, 0, 0);
		container.add(component, gbc);
	}
}