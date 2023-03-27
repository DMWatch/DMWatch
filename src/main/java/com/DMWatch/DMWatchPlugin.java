package com.DMWatch;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.DMWatch.data.GameItem;
import com.DMWatch.data.PartyPlayer;
import com.DMWatch.data.events.DMPartyBatchedChange;
import com.DMWatch.data.events.DMPartyMiscChange;
import com.DMWatch.ui.PlayerPanel;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.SpriteID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.FriendsChatMemberJoined;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PartyMemberAvatar;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import org.slf4j.LoggerFactory;

@Slf4j
@PluginDescriptor(
	name = "DMWatch"
)
public class DMWatchPlugin extends Plugin
{
	private static final String CHALLENGE = "Challenge in DM";
	private static final String BASE_DIRECTORY = System.getProperty("user.home") + "/.runelite/";
	private static final List<Integer> MENU_WIDGET_IDS = ImmutableList.of(
		WidgetInfo.FRIENDS_LIST.getGroupId(),
		WidgetInfo.IGNORE_LIST.getGroupId(),

		WidgetInfo.CHATBOX.getGroupId(),
		WidgetInfo.FRIENDS_CHAT.getGroupId(),
		WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId(),

		WidgetInfo.CLAN_MEMBER_LIST.getGroupId(),
		WidgetInfo.CLAN_GUEST_MEMBER_LIST.getGroupId()
	);
	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of(
		"Message", "Add ignore", "Remove friend", "Delete", "Kick", "Reject"
	);
	private static final BufferedImage ICON = ImageUtil.loadImageResource(DMWatchPlugin.class, "icon.png");
	@Getter
	private final Map<Long, PartyPlayer> partyMembers = new HashMap<>();
	@Inject
	CaseManager caseManager;
	@Inject
	ClientThread clientThread;
	@Inject
	DMWatchInputListener hotkeyListener;
	@Inject
	KeyManager keyManager;
	@Inject
	ItemManager itemManager;
	@Inject
	SpriteManager spriteManager;
	@Inject
	private Client client;
	@Inject
	private DMWatchConfig config;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private MenuManager menuManager;
	@Inject
	private ScheduledExecutorService executor;
	@Getter(AccessLevel.PACKAGE)
	private BufferedImage reportButton;
	@Inject
	private WSClient wsClient;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean hotKeyPressed;

	@Inject
	private PartyService partyService;
	@Getter
	private PartyPlayer myPlayer = null;
	private PartyPanel panel;
	private DMPartyBatchedChange currentChange = new DMPartyBatchedChange();
	private NavigationButton navButton;

	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private PluginManager pluginManager;

	private Instant lastLogout;

	private Logger dmwLogger;

	private LinkedHashSet<String> uniqueIDs;

	@Provides
	public DMWatchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DMWatchConfig.class);
	}

	@Override
	protected void startUp()
	{
		uniqueIDs = new LinkedHashSet<>();
		dmwLogger = setupLogger("DMWatchLogger", "DMWatch");
		panel = new PartyPanel(this, config);

		wsClient.registerMessage(DMPartyBatchedChange.class);

		if (config.playerOption() && client != null)
		{
			menuManager.addPlayerMenuItem(CHALLENGE);
		}

		navButton = NavigationButton.builder()
			.tooltip("DMWatch Panel")
			.icon(ICON)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);


		if (isInParty())
		{
			clientThread.invokeLater(() ->
			{
				myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
				partyService.send(new UserSync());
				partyService.send(partyPlayerAsBatchedChange());
			});
		}

		final Optional<Plugin> partyPlugin = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Party")).findFirst();
		if (partyPlugin.isPresent() && !pluginManager.isPluginEnabled(partyPlugin.get()))
		{
			pluginManager.setPluginEnabled(partyPlugin.get(), true);
		}

		keyManager.registerKeyListener(hotkeyListener);
		spriteManager.getSpriteAsync(SpriteID.CHATBOX_REPORT_BUTTON, 0, s -> reportButton = s);
		caseManager.refresh(this::colorAll);
		lastLogout = Instant.now();
	}

	@Override
	protected void shutDown()
	{
		lastLogout = null;
		uniqueIDs = new LinkedHashSet<>();
		clientToolbar.removeNavigation(navButton);

		if (config.playerOption() && client != null)
		{
			menuManager.removePlayerMenuItem(CHALLENGE);
		}

		keyManager.unregisterKeyListener(hotkeyListener);
		partyMembers.clear();
		wsClient.unregisterMessage(DMPartyBatchedChange.class);
		currentChange = new DMPartyBatchedChange();
	}

	@Subscribe
	public void onPartyChanged(final PartyChanged event)
	{
		partyMembers.clear();
		SwingUtilities.invokeLater(panel::renderSidebar);
		myPlayer = null;

		panel.updateParty();

		if (!isInParty())
		{
			panel.getPlayerPanelMap().clear();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged c)
	{
		if (!isInParty())
		{
			return;
		}

		if (c.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastLogout = Instant.now();
		}

		if (myPlayer == null)
		{
			myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
			final DMPartyBatchedChange ce = partyPlayerAsBatchedChange();
			partyService.send(ce);
			return;
		}

		if (c.getGameState() == GameState.LOGGED_IN)
		{
			DMPartyMiscChange e = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.W, client.getWorld());
			if (myPlayer.getWorld() != e.getV())
			{
				myPlayer.setWorld(e.getV());
				currentChange.getM().add(e);
			}
		}

		if (c.getGameState() == GameState.LOGIN_SCREEN)
		{

			if (myPlayer.getWorld() == 0)
			{
				return;
			}

			myPlayer.setWorld(0);
			currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.W, 0));
			currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.ACCOUNT_HASH, "Not Logged In"));

			if (currentChange.isValid())
			{
				currentChange.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
				currentChange.removeDefaults();
				partyService.send(currentChange);

				currentChange = new DMPartyBatchedChange();
			}
		}
	}

	@Schedule(
		period = 10,
		unit = ChronoUnit.SECONDS
	)
	private void checkIdle()
	{
		if (client.getGameState() != GameState.LOGIN_SCREEN)
		{
			return;
		}

		if (lastLogout != null && lastLogout.isBefore(Instant.now().minus(30, ChronoUnit.MINUTES))
			&& partyService.isInParty())
		{
			log.info("Leaving party due to inactivity");
			partyService.changeParty(null);
		}
	}

	@Subscribe
	public void onUserPart(final UserPart event)
	{
		final PartyPlayer removed = partyMembers.remove(event.getMemberId());
		if (removed != null)
		{
			SwingUtilities.invokeLater(() -> panel.removePartyPlayer(removed));
		}
	}

	@Subscribe
	public void onUserSync(final UserSync event)
	{
		if (myPlayer != null)
		{
			final DMPartyBatchedChange c = partyPlayerAsBatchedChange();
			if (c.isValid())
			{
				partyService.send(c);
			}
			return;
		}

		clientThread.invoke(() ->
		{
			myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
			final DMPartyBatchedChange c = partyPlayerAsBatchedChange();
			if (c.isValid())
			{
				partyService.send(c);
			}
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(DMWatchConfig.CONFIG_GROUP))
		{
			return;
		}

		if (event.getKey().equals(DMWatchConfig.PLAYER_OPTION))
		{
			if (!Boolean.parseBoolean(event.getOldValue()) && Boolean.parseBoolean(event.getNewValue()))
			{
				menuManager.addPlayerMenuItem(CHALLENGE);
			}
			else if (Boolean.parseBoolean(event.getOldValue()) && !Boolean.parseBoolean(event.getNewValue()))
			{
				menuManager.removePlayerMenuItem(CHALLENGE);
			}
		}
		else if (event.getKey().equals(DMWatchConfig.PLAYER_TEXT_COLOR))
		{
			colorAll();
		}


		if (event.getKey().equals("hideIDS"))
		{
			panel.renderSidebar();
		}
	}

	@Subscribe
	public void onFocusChanged(FocusChanged focusChanged)
	{
		if (!focusChanged.isFocused())
		{
			hotKeyPressed = false;
		}
	}

	private void colorAll()
	{
		clientThread.invokeLater(() -> {
			colorFriendsChat();
			colorClanChat();
			colorGuestClanChat();
		});
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned playerSpawned)
	{
		if (!config.notifyOnNearby())
		{
			return;
		}
		String name = playerSpawned.getPlayer().getName();
		this.alertPlayerWarning(name, false, AlertType.NEARBY);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
		String option = event.getOption();

		if (!MENU_WIDGET_IDS.contains(groupId) || !AFTER_OPTIONS.contains(option))
		{
			return;
		}

		if (!config.menuOption() || (!hotKeyPressed && config.useHotkey()))
		{
			return;
		}

		addEntryChallenge(event);
	}

	private void addEntryChallenge(MenuEntryAdded event)
	{
		for (MenuEntry me : client.getMenuEntries())
		{
			// don't add menu option if we've already added Challenge in DM
			if (CHALLENGE.equals(me.getOption()))
			{
				return;
			}
		}

		client.createMenuEntry(-1)
			.setOption(CHALLENGE)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.setParam0(event.getActionParam0())
			.setParam1(event.getActionParam1())
			.setIdentifier(event.getIdentifier());
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		Runnable color = null;
		if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD)
		{
			color = this::colorFriendsChat;
		}

		if (color != null)
		{
			clientThread.invokeLater(color);
		}
	}

	@Subscribe
	public void onFriendsChatMemberJoined(FriendsChatMemberJoined event)
	{
		if (!config.notifyOnJoin())
		{
			return;
		}
		String rsn = Text.toJagexName(event.getMember().getName());
		String local = client.getLocalPlayer().getName();
		if (rsn.equals(local))
		{
			return;
		}

		alertPlayerWarning(rsn, false, AlertType.FRIENDS_CHAT);
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event)
	{
		if (!config.notifyOnJoin())
		{
			return;
		}
		String rsn = Text.toJagexName(event.getClanMember().getName());
		String local = client.getLocalPlayer().getName();
		if (rsn.equals(local))
		{
			return;
		}

		alertPlayerWarning(rsn, false, AlertType.CLAN_CHAT);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		MenuAction action = event.getMenuAction();

		if ((action == MenuAction.RUNELITE || action == MenuAction.RUNELITE_PLAYER) && option.equals(CHALLENGE))
		{
			final String target;
			if (action == MenuAction.RUNELITE_PLAYER)
			{
				// The player id is included in the event, so we can use that to get the player name,
				// which avoids having to parse out the combat level and any icons preceding the name.
				Player player = client.getCachedPlayers()[event.getId()];
				if (player != null)
				{
					target = player.getName();
				}
				else
				{
					target = null;
				}
			}
			else
			{
				target = Text.removeTags(event.getMenuTarget());
			}

			if (target != null)
			{
				partyService.changeParty(getPrivateDM(target).toLowerCase().replaceAll(" ", ""));
			}
		}
	}

	private String getPrivateDM(String otherRSN)
	{
		String myName = Text.toJagexName(client.getLocalPlayer().getName().toLowerCase());
		int compare = myName.compareTo(otherRSN);

		if (compare > 0)
		{
			return myName + otherRSN;
		}
		else
		{
			return otherRSN + myName;
		}
	}

	@Schedule(period = 5, unit = ChronoUnit.MINUTES)
	public void refreshList()
	{
		caseManager.refresh(this::colorAll);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted ce)
	{
		if (ce.getCommand().equals("fw"))
		{
			final String rsn = String.join(" ", ce.getArguments());
			caseManager.get(rsn, (c) -> alertPlayerWarning(rsn, true, AlertType.NONE));
		}
	}

	@Subscribe
	private void onGameTick(GameTick e)
	{
		if (!isInParty() || client.getLocalPlayer() == null || partyService.getLocalMember() == null)
		{
			return;
		}

		// To reduce server load we should only process changes every X ticks
		if (client.getTickCount() % messageFreq(partyService.getMembers().size()) != 0)
		{
			return;
		}

		// First time logging in or they changed accounts so resend the entire player object
		if (myPlayer == null || !Objects.equals(client.getLocalPlayer().getName(), myPlayer.getUsername()))
		{
			myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
			final DMPartyBatchedChange c = partyPlayerAsBatchedChange();
			partyService.send(c);
			return;
		}

		if (myPlayer.getCombatLevel() != client.getLocalPlayer().getCombatLevel())
		{
			myPlayer.setCombatLevel(client.getLocalPlayer().getCombatLevel());
			currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.LVL, client.getLocalPlayer().getCombatLevel()));
		}

		if (client.getLocalPlayer() != null)
		{
			if (!myPlayer.getUserUnique().equals(getAccountID()))
			{
				currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.ACCOUNT_HASH, getAccountID()));
			}
		}

		DMPartyMiscChange e2 = null;
		if (caseManager.getByHWID(getHWID()) != null)
		{
			e2 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, caseManager.getByHWID(getHWID()).getStatus());
		}
		else if (caseManager.getByAccountHash(getAccountID()) != null)
		{
			e2 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, caseManager.getByAccountHash(getAccountID()).getStatus());
		}
		else if (caseManager.get(Text.toJagexName(client.getLocalPlayer().getName() == null ? "" : client.getLocalPlayer().getName())) != null)
		{
			e2 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, caseManager.getByHWID(client.getLocalPlayer().getName()).getStatus());
		}

		if (e2 != null && !myPlayer.getStatus().equals(e2.getS()))
		{
			myPlayer.setStatus(caseManager.getByHWID(getHWID()).getStatus());
			currentChange.getM().add(e2);
		}

		if (currentChange.isValid())
		{
			currentChange.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
			currentChange.removeDefaults();
			partyService.send(currentChange);

			currentChange = new DMPartyBatchedChange();
		}

		for (long memberID : partyMembers.keySet())
		{
			if (memberID == myPlayer.getMember().getMemberId())
			{
				continue;
			}

			if (partyMembers.get(memberID) != null)
			{
				String hwid, rid, rsn;
				hwid = partyMembers.get(memberID).getHWID();
				rid = partyMembers.get(memberID).getUserUnique();
				rsn = partyMembers.get(memberID).getUsername();

				if (hwid.equals("unknown"))
				{
					dmwLogger.info("UNUSUAL - HWID: {} | RID: {} | RSN: {}", hwid, rid, rsn);
					continue;
				}

				if (!uniqueIDs.contains(hwid + rid + rsn))
				{
					uniqueIDs.add(hwid + rid + rsn);
					dmwLogger.info("HWID: {} | RID: {} | RSN: {}", hwid, rid, rsn);
				}
			}
		}
	}

	private static int messageFreq(int partySize)
	{
		return Math.max(2, partySize - 6);
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged c)
	{
		if (myPlayer == null || !isInParty())
		{
			return;
		}

		if (c.getContainerId() == InventoryID.INVENTORY.getId())
		{
			myPlayer.setInventory(GameItem.convertItemsToGameItems(c.getItemContainer().getItems(), itemManager));
			int[] items = convertItemsToArray(c.getItemContainer().getItems());
			currentChange.setI(items);
		}
		else if (c.getContainerId() == InventoryID.EQUIPMENT.getId())
		{
			myPlayer.setEquipment(GameItem.convertItemsToGameItems(c.getItemContainer().getItems(), itemManager));
			int[] items = convertItemsToArray(c.getItemContainer().getItems());
			currentChange.setE(items);
		}
	}

	public void changeParty(String passphrase)
	{
		passphrase = passphrase.replace(" ", "-").trim();
		if (passphrase.length() == 0)
		{
			return;
		}

		for (int i = 0; i < passphrase.length(); ++i)
		{
			char ch = passphrase.charAt(i);
			if (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '-')
			{
				JOptionPane.showMessageDialog(panel.getControlsPanel(),
					"Party passphrase must be a combination of alphanumeric or hyphen characters.",
					"Invalid party passphrase",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		partyService.changeParty(passphrase);
		panel.updateParty();
	}

	public String getPartyPassphrase()
	{
		return partyService.getPartyPassphrase();
	}

	public void leaveParty()
	{
		partyService.changeParty(null);
		panel.updateParty();
	}

	@Subscribe
	public void onVarbitChanged(final VarbitChanged event)
	{
		if (myPlayer == null || !isInParty())
		{
			return;
		}

		if (event.getVarbitId() == Varbits.VENGEANCE_ACTIVE)
		{
			currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.V, event.getValue()));
		}
	}

	private int[] convertItemsToArray(Item[] items)
	{
		int[] eles = new int[items.length * 2];
		for (int i = 0; i < items.length * 2; i += 2)
		{
			if (items[i / 2] == null)
			{
				eles[i] = -1;
				eles[i + 1] = 0;
				continue;
			}

			eles[i] = items[i / 2].getId();
			eles[i + 1] = items[i / 2].getQuantity();
		}

		return eles;
	}

	private int[] convertGameItemsToArray(GameItem[] items)
	{
		int[] eles = new int[items.length * 2];
		for (int i = 0; i < items.length * 2; i += 2)
		{
			if (items[i / 2] == null)
			{
				eles[i] = -1;
				eles[i + 1] = 0;
				continue;
			}

			eles[i] = items[i / 2].getId();
			eles[i + 1] = items[i / 2].getQty();
		}

		return eles;
	}

	public DMPartyBatchedChange partyPlayerAsBatchedChange()
	{
		final DMPartyBatchedChange c = new DMPartyBatchedChange();
		if (myPlayer == null)
		{
			return c;
		}

		// Inventories
		c.setI(convertGameItemsToArray(myPlayer.getInventory()));
		c.setE(convertGameItemsToArray(myPlayer.getEquipment()));

		// misc
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.V, myPlayer.getIsVenged()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.U, myPlayer.getUsername()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.W, myPlayer.getWorld()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.P, myPlayer.getPluginEnabled()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.HWID, getHWID()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.ACCOUNT_HASH, getAccountID()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, myPlayer.getStatus()));
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.LVL, myPlayer.getCombatLevel()));

		c.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
		c.removeDefaults();

		return c;
	}

	@Subscribe
	public void onDMPartyBatchedChange(DMPartyBatchedChange e)
	{
		// create new PartyPlayer for this member if they don't already exist
		final PartyPlayer player = partyMembers.computeIfAbsent(e.getMemberId(), k -> new PartyPlayer(partyService.getMemberById(e.getMemberId())));

		clientThread.invoke(() ->
		{
			e.process(player, itemManager);

			SwingUtilities.invokeLater(() -> {
				final PlayerPanel playerPanel = panel.getPlayerPanelMap().get(e.getMemberId());
				if (playerPanel != null)
				{
					playerPanel.updatePlayerData(player, e.hasBreakingBannerChange());
					return;
				}

				panel.drawPlayerPanel(player);
			});
		});
	}

	@Subscribe
	public void onPartyMemberAvatar(PartyMemberAvatar e)
	{
		if (isLocalPlayer(e.getMemberId()) || partyMembers.get(e.getMemberId()) == null)
		{
			return;
		}

		final PartyPlayer player = partyMembers.get(e.getMemberId());
		player.getMember().setAvatar(e.getImage());
		SwingUtilities.invokeLater(() -> {
			final PlayerPanel p = panel.getPlayerPanelMap().get(e.getMemberId());
			if (p != null)
			{
				p.getBanner().refreshStats();
			}
		});
	}

	public boolean isInParty()
	{
		return partyService.isInParty();
	}

	public boolean isLocalPlayer(long id)
	{
		return partyService.getLocalMember() != null && partyService.getLocalMember().getMemberId() == id;
	}

	private String getHWID()
	{
		String toEncrypt = System.getenv("COMPUTERNAME") + System.getProperty("user.name") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL");
		return getEncrypt(toEncrypt);
	}

	private String getAccountID()
	{
		if (client != null)
		{
			String toEncrypt = String.valueOf(client.getAccountHash());
			return getEncrypt(toEncrypt);
		}

		return "Unknown";
	}

	private String getEncrypt(String input)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes());
			StringBuilder hexString = new StringBuilder();

			byte[] byteData = md.digest();

			for (byte aByteData : byteData)
			{
				String hex = Integer.toHexString(0xff & aByteData);
				if (hex.length() == 1)
				{
					hexString.append('0');
				}
				hexString.append(hex);
			}

			String s = hexString.substring(0, 16).toLowerCase();

			String s1 = s.substring(0, 4);
			String s2 = s.substring(4, 8);
			String s3 = s.substring(8, 12);
			String s4 = s.substring(12, 16);

			return s1 + "-" + s2 + "-" + s3 + "-" + s4;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "Error";
		}
	}

	private Logger setupLogger(String loggerName, String subFolder)
	{
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%d{HH:mm:ss} %msg%n");
		encoder.start();

		String directory = BASE_DIRECTORY + subFolder + "/";

		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		appender.setFile(directory + "latest.log");
		appender.setAppend(true);
		appender.setEncoder(encoder);
		appender.setContext(context);

		TimeBasedRollingPolicy<ILoggingEvent> logFilePolicy = new TimeBasedRollingPolicy<>();
		logFilePolicy.setContext(context);
		logFilePolicy.setParent(appender);
		logFilePolicy.setFileNamePattern(directory + "chatlog_%d{yyyy-MM-dd}.log");
		logFilePolicy.setMaxHistory(30);
		logFilePolicy.start();

		appender.setRollingPolicy(logFilePolicy);
		appender.start();

		Logger logger = context.getLogger(loggerName);
		logger.detachAndStopAllAppenders();
		logger.setAdditive(false);
		logger.setLevel(Level.INFO);
		logger.addAppender(appender);

		return logger;
	}

	private void alertPlayerWarning(String rsn, boolean notifyClear, AlertType alertType)
	{
		rsn = Text.toJagexName(rsn);
		Case dmwCase = caseManager.get(rsn);
		ChatMessageBuilder response = new ChatMessageBuilder();
		response.append(alertType.getMessage())
			.append(ChatColorType.HIGHLIGHT)
			.append(rsn)
			.append(ChatColorType.NORMAL);

		if (dmwCase == null && !notifyClear)
		{
			return;
		}
		else
		{
//			if (dmwCase.getStatus().equals("1"))
//			{
//				response.append(" is in good standing on DMWatch.");
//			}
//			else
			if (dmwCase.getStatus().equals("2"))
			{
				response.append(" is accused.");
			}
			else if (dmwCase.getStatus().equals("3"))
			{
				response.append(String.format(" is a scammer for ", dmwCase.getReason()))
					.append(ChatColorType.HIGHLIGHT)
					.append(dmwCase.getReason());
				if (dmwCase.getDate().getTime() > 0)
				{
					response.append(" ")
						.append(ChatColorType.NORMAL)
						.append("on " + dmwCase.niceDate())
						.append(".");
				}
				else
				{
					response.append(ChatColorType.NORMAL)
						.append(".");
				}
			}
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(response.build())
			.build());
	}

	private void colorFriendsChat()
	{
		Widget ccList = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);
		if (ccList != null)
		{
			illiteratePlayerWidgets(ccList);
		}
	}

	private void colorClanChat()
	{
		Widget clanChatList = client.getWidget(WidgetInfo.CLAN_MEMBER_LIST);
		if (clanChatList != null)
		{
			illiteratePlayerWidgets(clanChatList);
		}
	}

	private void colorGuestClanChat()
	{
		Widget guestClanChatList = client.getWidget(WidgetInfo.CLAN_MEMBER_LIST);
		if (guestClanChatList != null)
		{
			illiteratePlayerWidgets(guestClanChatList);
		}
	}

	private void illiteratePlayerWidgets(Widget chatWidget)
	{
		Widget[] players = chatWidget.getDynamicChildren();
		for (int i = 0; i < players.length; i += 3)
		{
			Widget player = players[i];
			if (player == null)
			{
				continue;
			}

			Case rwCase = caseManager.get(player.getText());
			if (rwCase == null)
			{
				continue;
			}

			player.setTextColor(config.playerTextColor().getRGB());
			player.revalidate();
		}
	}


	private enum AlertType
	{
		NONE(""),
		FRIENDS_CHAT("Friends chat member, "),
		CLAN_CHAT("Clan chat member, "),
		NEARBY("Nearby player, ");

		private final String message;

		AlertType(String message)
		{
			this.message = message;
		}

		public String getMessage()
		{
			return message;
		}
	}
}
