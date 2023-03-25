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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import com.DMWatch.DMWatchPlugin;
import javax.swing.SwingUtilities;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;


// A copy of the controls from the `net.runelite.client.plugins.party.PartyPanel` class
public class ControlsPanel extends JPanel
{
	private final JButton joinPartyButton = new JButton();
	private static ImageIcon HELP_ICON;
	private static ImageIcon HELP_HOVER_ICON;


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
			if (!plugin.isInParty())
			{
				plugin.changeParty("DMW");
			} else {
				plugin.leaveParty();
			}
		});

		BufferedImage helpIcon = ImageUtil.loadImageResource(DMWatchPlugin.class, "discord-mark-white.png");
		HELP_ICON = new ImageIcon(helpIcon);
		HELP_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(helpIcon, 0.53f));

		c.gridx = 1;
		c.gridy = 0;

		JLabel helpButton = new JLabel(HELP_ICON);

		this.add(helpButton, c);
		helpButton.setToolTipText("Click to join our discord");
		helpButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					LinkBrowser.browse("https://discord.com/invite/dm");
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				helpButton.setIcon(HELP_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				helpButton.setIcon(HELP_ICON);
			}
		});

		updateControls();
	}

	public void updateControls()
	{
		joinPartyButton.setText(plugin.isInParty() ? "Leave DMW" : "Join DMW");
	}
}
