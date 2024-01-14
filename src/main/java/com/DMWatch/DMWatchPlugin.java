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
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import static net.runelite.api.MenuAction.*;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.FriendsChatMemberJoined;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetUtil;
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
import net.runelite.client.menus.MenuManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsConfig;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsPlugin;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import org.slf4j.LoggerFactory;

@Slf4j
@PluginDescriptor(
	name = "DMWatch"
)
@PluginDependency(PlayerIndicatorsPlugin.class)
public class DMWatchPlugin extends Plugin
{
	private static final String CHALLENGE = "Challenge in DM";
	private static final String BASE_DIRECTORY = System.getProperty("user.home") + "/.runelite/";
	public static final File DMWATCH_DIR = new File(BASE_DIRECTORY, "DMWatch");

	private static final List<Integer> MENU_WIDGET_IDS = ImmutableList.of(
		InterfaceID.FRIEND_LIST,
		InterfaceID.IGNORE_LIST,

		InterfaceID.CHATBOX,
		InterfaceID.FRIENDS_CHAT,
		InterfaceID.PRIVATE_CHAT,

		ComponentID.CLAN_MEMBERS,
		ComponentID.CLAN_GUEST_MEMBERS
	);

	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of(
		"Message", "Add ignore", "Remove friend", "Delete", "Kick", "Reject"
	);

	private static final BufferedImage ICON = ImageUtil.loadImageResource(DMWatchPlugin.class, "icon.png");

	@Getter
	private final Map<Long, PartyPlayer> partyMembers = new HashMap<>();

	@Inject
	Gson gson;

	@Inject
	CaseManager caseManager;

	@Inject
	ClientThread clientThread;

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
	private WSClient wsClient;

	@Inject
	private PartyService partyService;

	@Getter
	private PartyPlayer myPlayer = null;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private PartyMemberTierOverlay partyMemberTierOverlay;

	@Inject
	private PlayerMemberTileTierOverlay playerMemberTileTierOverlay;

	@Inject
	private PartyMemberIndicatorService partyMemberIndicatorService;

	@Inject
	private PlayerIndicatorsPlugin playerIndicatorsPlugin;

	// globals
	private Instant lastLogout;
	private Instant lastNotify;
	private Logger dmwLogger;
	private LinkedHashSet<String> uniqueIDs;
	private PartyPanel panel;
	private DMPartyBatchedChange currentChange = new DMPartyBatchedChange();
	private NavigationButton navButton;
	private HashMap<String, Instant> nameNotifier;
	private Instant lastSync;
	private int ticksLoggedIn;
	private boolean menuOptionEnabled;
	@Setter
	private String opponent;
	@Getter
	private boolean renderOnSelf;

	@Getter
	private HashSet<String> localScammers;

	@Getter
	private boolean showFriendRanks;
	@Getter
	private boolean showClanRanks;

	@Getter
	@Setter
	private String searchBarText;

	@Provides
	public DMWatchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DMWatchConfig.class);
	}

	@Override
	protected void startUp()
	{
		reset(true);

		wsClient.registerMessage(DMPartyBatchedChange.class);
		navButton = NavigationButton.builder()
			.tooltip("DMWatch Panel")
			.icon(ICON)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (config.playerOption() && client != null)
		{
			addRemovePlayerMenu(true);
		}

		final Optional<Plugin> partyPlugin = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Party")).findFirst();
		if (partyPlugin.isPresent() && !pluginManager.isPluginEnabled(partyPlugin.get()))
		{
			pluginManager.setPluginEnabled(partyPlugin.get(), true);
		}
	}

	@Override
	protected void shutDown()
	{
		reset(false);

		wsClient.unregisterMessage(DMPartyBatchedChange.class);
		clientToolbar.removeNavigation(navButton);

		if (config.playerOption() && client != null)
		{
			addRemovePlayerMenu(false);
		}
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
		if (c.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastLogout = Instant.now();
		}

		if (config.discordNotify() && c.getGameState() == GameState.LOGGED_IN)
		{
			ticksLoggedIn = 0;
		}

		if (!isInParty())
		{
			return;
		}

		if (isInParty() && myPlayer == null)
		{
			myPlayer = new PartyPlayer(partyService.getLocalMember(), client, config, itemManager);
			final DMPartyBatchedChange ce = partyPlayerAsBatchedChange();
			partyService.send(ce);
			return;
		}

		if (c.getGameState() == GameState.LOGGED_IN)
		{

			int world = getWorld();
			DMPartyMiscChange e = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.W, world);

			if (myPlayer.getWorld() != e.getV())
			{
				myPlayer.setWorld(e.getV());
				currentChange.getM().add(e);
			}
		}

		if (c.getGameState() == GameState.HOPPING)
		{
			myPlayer.setIsVenged(0);
			currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.V, myPlayer.getIsVenged()));
		}

		if (c.getGameState() == GameState.LOGIN_SCREEN)
		{
			if (myPlayer.getWorld() == 0)
			{
				return;
			}

			int world = getWorld();

			myPlayer.setWorld(world);
			myPlayer.setIsVenged(0);

			if (isInParty())
			{
				final DMPartyBatchedChange cleanUserInfo = partyPlayerAsBatchedChange();
				cleanUserInfo.setI(new int[0]);
				cleanUserInfo.setE(new int[0]);
				cleanUserInfo.setM(Collections.emptySet());
				partyService.send(cleanUserInfo);
			}

			if (currentChange.isValid())
			{
				currentChange.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
				currentChange.removeDefaults();
				partyService.send(currentChange);

				currentChange = new DMPartyBatchedChange();
			}
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
			myPlayer = new PartyPlayer(partyService.getLocalMember(), client, config, itemManager);
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
		if(event.getGroup().equals("runelite")) {
			if (event.getKey().equals("playerindicatorsplugin")) {
				if (event.getNewValue().equals("false")) {
					showClanRanks = false;
					showFriendRanks = false;
				} else {
					showClanRanks = showClanRanks();
					showFriendRanks = showFriendsRanks();
				}
			}
		}

		if (event.getGroup().equals("playerindicators")) {
			showClanRanks = showClanRanks();
			showFriendRanks = showFriendsRanks();
		}

		if (!event.getGroup().equals(DMWatchConfig.CONFIG_GROUP))
		{
			return;
		}

		if (event.getKey().equals(DMWatchConfig.MENU_OPTION))
		{
			addRemoveMenuOption();
		}

		if (event.getKey().equals("drawOnSelf"))
		{
			renderOnSelf = config.drawOnSelf();
		}

		if (event.getKey().equals(DMWatchConfig.PLAYER_OPTION))
		{
			addRemovePlayerMenu();
		}

		else if (event.getKey().equals(DMWatchConfig.PLAYER_TEXT_COLOR))
		{
			colorAll();
		}

		if (event.getKey().equals("hideIDS"))
		{
			panel.renderSidebar();
		}
		if (event.getKey().equals("hideMyWorld"))
		{
			hideWorld();
		}
		if (event.getKey().equals("notifyReminder"))
		{
			nameNotifier.clear();
		}
	}

	private void addRemovePlayerMenu(boolean add)
	{
		if (add)
		{
			menuManager.addPlayerMenuItem(CHALLENGE);
		} else{
			menuManager.removePlayerMenuItem(CHALLENGE);
		}
	}

	private void addRemovePlayerMenu()
	{
		addRemovePlayerMenu(config.playerOption());
	}

	private void addRemoveMenuOption()
	{
		menuOptionEnabled = config.menuOption();
	}

	private int getWorld()
	{
		if (config.hideWorld())
		{
			return -1;
		}
		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			return 0;
		}
		return client.getWorld();
	}

	private void hideWorld()
	{
		myPlayer.setWorld(getWorld());
		currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.W, myPlayer.getWorld()));

		if (currentChange.isValid())
		{
			partyService.send(currentChange);
			currentChange = new DMPartyBatchedChange();
		}
	}

	private void colorAll()
	{
		clientThread.invoke(() -> {
			colorFriendsChat();
			colorClanChat();
			colorGuestClanChat();
		});
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned playerSpawned)
	{
		if (!config.notifyOnNearby()) return;
		String name = playerSpawned.getPlayer().getName();
		alertPlayerWarning(name, false, AlertType.NEARBY);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!menuOptionEnabled) return;

		final int groupId = WidgetUtil.componentToInterface(event.getActionParam1());
		String option = event.getOption();

		if (!MENU_WIDGET_IDS.contains(groupId) || !AFTER_OPTIONS.contains(option))
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
		if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD)
		{
			colorFriendsChat();
		}
	}

	@Subscribe
	public void onFriendsChatMemberJoined(FriendsChatMemberJoined event)
	{
		if (!config.notifyOnJoin()) return;

		String rsn = Text.toJagexName(event.getMember().getName());
		String local = client.getLocalPlayer().getName();
		if (rsn.equals(local)) return;

		alertPlayerWarning(rsn, false, AlertType.FRIENDS_CHAT);
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event)
	{
		if (!config.notifyOnJoin()) return;

		String rsn = Text.toJagexName(event.getClanMember().getName());
		String local = client.getLocalPlayer().getName();
		if (rsn.equals(local)) return;

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
				opponent = Text.toJagexName(target).toLowerCase();
				partyService.changeParty(getPrivateDM(target));
				if (config.openDMWatchSidePanelOnChallenge())
				{
					SwingUtilities.invokeLater(() ->
					{
						SwingUtilities.invokeLater(() ->
						{
							clientToolbar.openPanel(navButton);
						});
					});
				}
			}
		}
	}

	private String getPrivateDM(String otherRSN)
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return "Error";
		}

		String theirName = Text.toJagexName(otherRSN).replaceAll(" ", "").toLowerCase();
		String myName = Text.toJagexName(client.getLocalPlayer().getName()).replaceAll(" ", "").toLowerCase();
		int compare = myName.compareTo(theirName);

		if (compare < 0)
		{
			return myName + theirName;
		}
		else
		{
			return theirName + myName;
		}
	}


	@Subscribe
	public void onCommandExecuted(CommandExecuted ce)
	{
		if (ce.getCommand().equals("fw"))
		{
			final String rsn = String.join(" ", ce.getArguments());
			caseManager.get(rsn, (c) -> alertPlayerWarning(rsn, true, AlertType.NONE));
		}

		if (ce.getCommand().equals("updatefw"))
		{
			refreshList();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();

		for (MenuEntry entry : menuEntries)
		{
			MenuAction type = entry.getType();

			if (type == WALK
				|| type == WIDGET_TARGET_ON_PLAYER
				|| type == ITEM_USE_ON_PLAYER
				|| type == PLAYER_FIRST_OPTION
				|| type == PLAYER_SECOND_OPTION
				|| type == PLAYER_THIRD_OPTION
				|| type == PLAYER_FOURTH_OPTION
				|| type == PLAYER_FIFTH_OPTION
				|| type == PLAYER_SIXTH_OPTION
				|| type == PLAYER_SEVENTH_OPTION
				|| type == PLAYER_EIGHTH_OPTION
				|| type == RUNELITE_PLAYER)
			{
				Player[] players = client.getCachedPlayers();
				Player player = null;

				int identifier = entry.getIdentifier();

				// 'Walk here' identifiers are offset by 1 because the default
				// identifier for this option is 0, which is also a player index.
				if (type == WALK)
				{
					identifier--;
				}

				if (identifier >= 0 && identifier < players.length)
				{
					player = players[identifier];
				}

				if (player == null)
				{
					continue;
				}

				PartyMemberIndicatorService.Decorations decorations = partyMemberIndicatorService.getDecorations(player);
				if (decorations == null)
				{
					continue;
				}

				String oldTarget = entry.getTarget();
				String newTarget = decorateTarget(oldTarget, decorations);

				entry.setTarget(newTarget);
			}
		}
	}

	private String decorateTarget(String oldTarget, PartyMemberIndicatorService.Decorations decorations)
	{
		String newTarget = oldTarget;

		if (decorations.getColor() != null)
		{
			// strip out existing <col...
			int idx = oldTarget.indexOf('>');
			if (idx != -1)
			{
				newTarget = oldTarget.substring(idx + 1);
			}

			newTarget = ColorUtil.prependColorTag(newTarget, decorations.getColor());
		}

		return newTarget;
	}

	@Subscribe
	private void onGameTick(GameTick e)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		ticksLoggedIn++;
		if (ticksLoggedIn == 5 && config.discordNotify() && shouldRemind()) {
			lastNotify = Instant.now();
			ChatMessageBuilder response = new ChatMessageBuilder();
			response.append(ChatColorType.NORMAL)
				.append("Join ")
				.append(ChatColorType.HIGHLIGHT)
				.append("DMWatch's discord")
				.append(ChatColorType.NORMAL)
				.append(" by clicking ")
				.append(ChatColorType.HIGHLIGHT)
				.append("here!");

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(response.build().replaceAll("colHIGHLIGHT", "col=c41ab0"))
				.build());

			response = new ChatMessageBuilder();
			response.append(ChatColorType.NORMAL)
				.append("Or by ")
				.append(ChatColorType.HIGHLIGHT)
				.append("clicking")
				.append(ChatColorType.NORMAL)
				.append(" the discord icon in ")
				.append(ChatColorType.HIGHLIGHT)
				.append("side panel!");

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(response.build())
				.build());
		}

		Widget chatWidget = client.getWidget(ComponentID.CHATBOX_MESSAGE_LINES);
		if (chatWidget != null && config.discordNotify())
		{
			for (Widget w: chatWidget.getDynamicChildren())
			{
				if (Text.removeTags(w.getText()).contains("Join DMWatch's discord by clicking here!")
					|| Text.removeTags(w.getText()).contains("Or by clicking the discord icon in side panel!"))
				{
					clientThread.invokeLater(() -> {
						w.setAction(1, "Open Discord");
						w.setOnOpListener((JavaScriptCallback) this::click);
						w.setHasListener(true);
						w.setNoClickThrough(true);
						w.revalidate();
					});
				} else {
					clientThread.invokeLater(() -> {
						w.setHasListener(false);
						w.setNoClickThrough(false);
						w.revalidate();
					});
				}
			}
		}

		if (!isInParty() || partyService.getLocalMember() == null)
		{
			return;
		}

		if (partyMembers.size() > 1)
		{
			clientThread.invoke(() -> SwingUtilities.invokeLater(() -> panel.renderSidebar()));
		}

		// To reduce server load we should only process changes every X ticks
		if (client.getTickCount() % messageFreq(partyService.getMembers().size()) != 0)
		{
			return;
		}

		// First time logging in or they changed accounts so resend the entire player object
		if (myPlayer == null || !Objects.equals(client.getLocalPlayer().getName(), myPlayer.getUsername()))
		{
			myPlayer = new PartyPlayer(partyService.getLocalMember(), client, config, itemManager);
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
				myPlayer.setUserUnique(getAccountID());
				currentChange.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.ACCOUNT_HASH, myPlayer.getUserUnique()));
			}
		}

		DMPartyMiscChange e2 = null;
		DMPartyMiscChange e3 = null;
		if (caseManager.getByHWID(getHWID()) != null)
		{
			e2 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.TIER, caseManager.getByHWID(getHWID()).getStatus());
			e3 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, caseManager.getByHWID(getHWID()).getReason());
		}
		else if (caseManager.getByAccountHash(getAccountID()) != null)
		{
			e2 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.TIER, caseManager.getByAccountHash(getAccountID()).getStatus());
			e3 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, caseManager.getByAccountHash(getAccountID()).getReason());
		}
		else if (caseManager.get(Text.toJagexName(client.getLocalPlayer().getName() == null ? "" : client.getLocalPlayer().getName())) != null)
		{
			e2 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.TIER, caseManager.get(client.getLocalPlayer().getName()).getStatus());
			e3 = new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.REASON, caseManager.get(client.getLocalPlayer().getName()).getReason());
		}

		if (e2 != null && !myPlayer.getStatus().equals(e2.getS()))
		{
			myPlayer.setReason(e3.getS());
			myPlayer.setStatus(e2.getS());
			currentChange.getM().add(e2);
			currentChange.getM().add(e3);
		}

		if (currentChange.isValid())
		{
			currentChange.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
			currentChange.removeDefaults();
			partyService.send(currentChange);

			currentChange = new DMPartyBatchedChange();
		}
	}

	protected void click(ScriptEvent ev)
	{
		LinkBrowser.browse("https://discord.gg/dmwatch");
		log.info("Opened a link to DMWatch discord");
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage("Opened an invite link to DMWatch discord. You can disable this in the DMWatch config.")
			.build());
	}

	private static int messageFreq(int partySize)
	{
		return Math.min(Math.max(2, partySize - 6), 8);
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
		else if (c.getContainerId() == InventoryID.TRADE.getId())
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

		if (passphrase.equals("DMWatch"))
		{
			clientThread.invoke(() -> SwingUtilities.invokeLater(() -> panel.getSearchBar().setText("")));
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
		if (isInParty())
		{
			opponent = "";
			partyService.changeParty(null);
			panel.updateParty();
		}
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
			myPlayer.setIsVenged(event.getValue());
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
		c.getM().add(new DMPartyMiscChange(DMPartyMiscChange.PartyMisc.TIER, myPlayer.getStatus()));
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
		if (client != null && client.getLocalPlayer() != null)
		{
			String toEncrypt = String.valueOf(client.getAccountHash());
			return getEncrypt(toEncrypt);
		}

		return "";
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
		logFilePolicy.setFileNamePattern(directory + "DMWatchlog_%d{yyyy-MM-dd}.log");
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
		if (caseManager.getListSize() == 0)
		{
			return;
		}
		rsn = Text.toJagexName(rsn);
		Case dmwCase = caseManager.get(rsn);

		ChatMessageBuilder response = new ChatMessageBuilder();
		response.append(alertType.getMessage())
			.append(ChatColorType.HIGHLIGHT)
			.append(rsn)
			.append(ChatColorType.NORMAL);

		if (dmwCase == null && !notifyClear)
		{
			// do nothing
		}
		else if (dmwCase == null)
		{
			response.append(" is not on DMWatch.");
		}
		else
		{
			if (dmwCase.getStatus().equals("2"))
			{
				response.append(" is accused");
			}
			else if (dmwCase.getStatus().equals("3"))
			{
				response.append(" is a scammer [")
					.append(ChatColorType.HIGHLIGHT)
					.append(dmwCase.getReason());
				response.append(ChatColorType.NORMAL)
					.append("].");
			}
			else
			{
				if (notifyClear)
				{
					String message = " is a " + msg(dmwCase.getStatus()) +
						((dmwCase.getReason() == null || dmwCase.getReason().isEmpty()) ? "" :
							" with a note: '" + dmwCase.getReason() + "'");
					response.append(ChatColorType.NORMAL).append(message);
				}
				else
				{
					return;
				}
			}

			if (nameNotifier.containsKey(rsn))
			{
				if (config.notifyReminder() == -1)
				{
					return;
				}
				if (Instant.now().isAfter(nameNotifier.get(rsn)))
				{
					chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(response.build())
						.build());
					nameNotifier.put(rsn, Instant.now().plus(Duration.ofMinutes(config.notifyReminder())));
				}
			}
			else
			{
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(response.build())
					.build());

				nameNotifier.put(rsn, Instant.now().plus(Duration.ofMinutes(config.notifyReminder())));
			}
		}
	}

	public String msg(String status)
	{
		switch (status)
		{
			case "0":
				return "User";
			case "1":
				return "Smiley";
			case "9":
				return "Recruit";
			case "10":
				return "Corporal";
			case "11":
				return "Sergeant";
			case "2":
				return "Accused";
			case "3":
				return "Scammer";
			case "4":
				return "Lieutenant";
			case "5":
				return "Captain";
			case "6":
			case "7":
				return "Streamer";
			case "8":
				return "General";
			default:
				return "Unknown";
		}
	}

	private void colorFriendsChat()
	{
		Widget ccList = client.getWidget(ComponentID.FRIENDS_CHAT_LIST);
		if (ccList != null)
		{
			illiteratePlayerWidgets(ccList);
		}
	}

	private void colorClanChat()
	{
		Widget clanChatList = client.getWidget(ComponentID.CLAN_MEMBERS);
		if (clanChatList != null)
		{
			illiteratePlayerWidgets(clanChatList);
		}
	}

	private void colorGuestClanChat()
	{
		Widget guestClanChatList = client.getWidget(ComponentID.CLAN_GUEST_MEMBERS);
		if (guestClanChatList != null)
		{
			illiteratePlayerWidgets(guestClanChatList);
		}
	}

	@Schedule(
		period = 500,
		unit = ChronoUnit.MILLIS
	)
	public void addUsersToLog()
	{
		if (!isInParty())
		{
			return;
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

				Case c = caseManager.getByHWID(hwid);
				int listSize = localScammers.size();
				if (c != null)
				{
					if (c.getStatus().equals("3"))
					{
						if (!opponent.equalsIgnoreCase(c.getNiceRSN()))
						{
							localScammers.add(rsn);
							if (listSize != localScammers.size())
							{
								ChatMessageBuilder response = new ChatMessageBuilder();
								response.append("Challenged player is a scammer [")
									.append(ChatColorType.HIGHLIGHT)
									.append(rsn)
									.append(ChatColorType.NORMAL)
									.append("]");

								chatMessageManager.queue(QueuedMessage.builder()
									.type(ChatMessageType.CONSOLE)
									.runeLiteFormattedMessage(response.build())
									.build());
							}
						}
					}
				} else
				{
					c = caseManager.getByAccountHash(rid);
					if (c != null)
					{
						if (c.getStatus().equals("3"))
						{
							if (!opponent.equalsIgnoreCase(c.getNiceRSN()))
							{
								localScammers.add(rsn);
								if (listSize != localScammers.size())
								{
									ChatMessageBuilder response = new ChatMessageBuilder();
									response.append("Challenged player is a scammer [")
										.append(ChatColorType.HIGHLIGHT)
										.append(rsn)
										.append(ChatColorType.NORMAL)
										.append("]");

									chatMessageManager.queue(QueuedMessage.builder()
										.type(ChatMessageType.CONSOLE)
										.runeLiteFormattedMessage(response.build())
										.build());
								}
							}
						}
					}
				}

				if (!uniqueIDs.contains(hwid + rid + rsn))
				{
					if (hwid.equals("unknown"))
					{
						dmwLogger.info("Unusual - hwid:{} hash:{} rsn:{}", hwid, rid, rsn);
					}
					else
					{
						dmwLogger.info("hwid:{} hash:{} rsn:{}", hwid, rid, rsn);
					}
					uniqueIDs.add(hwid + rid + rsn);
				}
			}
		}
	}

	public ConcurrentHashMap<String, HashSet<String>> getMappings()
	{
		return caseManager.getMappings();
	}

	private void illiteratePlayerWidgets(Widget chatWidget)
	{
		if (chatWidget == null || chatWidget.getChildren() == null)
		{
			return;
		}

		for (int i = 0; i < chatWidget.getChildren().length; i += 1)
		{
			Widget listWidget = chatWidget.getChild(i);
			String memberName = listWidget.getText();

			if (memberName.isEmpty())
			{
				continue;
			}
			if (caseManager.getMappings().get("3").contains(Text.toJagexName(memberName.toLowerCase())))
			{
				listWidget.setTextColor(Color.RED.getRGB());
			}
		}
	}

	private boolean showFriendsRanks() {
		Optional<Plugin> playerIndicators = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Player Indicators")).findFirst();
		if (playerIndicators.isPresent() && pluginManager.isPluginEnabled(playerIndicators.get()))
		{
			if (pluginManager.isPluginEnabled(playerIndicators.get())) {
				PlayerIndicatorsConfig config1 = (PlayerIndicatorsConfig) pluginManager.getPluginConfigProxy(playerIndicators.get());
				if (config1.showFriendsChatRanks()) {
					return  true;
				}
			}
		}

		return false;
	}

	private boolean showClanRanks() {
		final Optional<Plugin> playerIndicators = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Player Indicators")).findFirst();
		if (playerIndicators.isPresent() && pluginManager.isPluginEnabled(playerIndicators.get()))
		{
			if (pluginManager.isPluginEnabled(playerIndicators.get())) {
				PlayerIndicatorsConfig config1 = (PlayerIndicatorsConfig) pluginManager.getPluginConfigProxy(playerIndicators.get());
				if (config1.showClanChatRanks()) {
					return  true;
				}
			}
		}

		return false;
	}

	private boolean shouldRemind()
	{
		if (lastNotify == null)
			return true;
		if (Instant.now().isAfter(lastNotify.plus(120, ChronoUnit.SECONDS)))
			return true;
		return false;
	}

	private void reset(boolean turningOn)
	{
		ticksLoggedIn = 0;
		DMWATCH_DIR.mkdirs();
		uniqueIDs = new LinkedHashSet<>();
		dmwLogger = setupLogger("DMWatchLogger", "DMWatch");
		panel = new PartyPanel(this, config);
		nameNotifier = new HashMap<>();
		showFriendRanks = showFriendsRanks();
		showClanRanks = showClanRanks();
		localScammers = new HashSet<>();
		lastNotify = null;
		addRemoveMenuOption();
		renderOnSelf = config.drawOnSelf();
		if (turningOn)
		{
			overlayManager.add(playerMemberTileTierOverlay);
			overlayManager.add(partyMemberTierOverlay);

			caseManager.refresh(this::colorAll);
			lastSync = Instant.now();
			lastLogout = Instant.now();

			if (isInParty())
			{
				clientThread.invokeLater(() ->
				{
					myPlayer = new PartyPlayer(partyService.getLocalMember(), client, config, itemManager);
					partyService.send(new UserSync());
					partyService.send(partyPlayerAsBatchedChange());
				});
			}
		}
		else
		{
			overlayManager.remove(playerMemberTileTierOverlay);
			overlayManager.remove(partyMemberTierOverlay);

			lastSync = null;
			lastLogout = null;

			partyMembers.clear();
			currentChange = new DMPartyBatchedChange();

			if (isInParty())
			{
				final DMPartyBatchedChange cleanUserInfo = partyPlayerAsBatchedChange();
				cleanUserInfo.setI(new int[0]);
				cleanUserInfo.setE(new int[0]);
				cleanUserInfo.setM(Collections.emptySet());
				partyService.send(cleanUserInfo);
			}
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

	@Schedule(period = 30, unit = ChronoUnit.SECONDS)
	public void removeUsersFromPartyPanel()
	{
		if (!isInParty())
		{
			return;
		}
		final Set<Long> members = partyService.getMembers().stream()
			.map(PartyMember::getMemberId)
			.collect(Collectors.toSet());

		for (final long memberId : partyMembers.keySet())
		{
			if (!members.contains(memberId))
			{
				SwingUtilities.invokeLater(() -> panel.removePartyPlayer(partyMembers.get(memberId)));
				partyMembers.remove(memberId);
			}
		}
	}

	@Schedule(
		period = 10,
		unit = ChronoUnit.SECONDS
	)
	public void checkIdle()
	{
		if (client.getGameState() != GameState.LOGIN_SCREEN)
		{
			return;
		}

		if (lastLogout != null && lastLogout.isBefore(Instant.now().minus(10, ChronoUnit.MINUTES))
			&& partyService.isInParty())
		{
			log.info("Leaving party due to inactivity");
			partyService.changeParty(null);
		}
	}

	@Schedule(period = 1, unit = ChronoUnit.SECONDS)
	public void refreshList()
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;

		// if using default end point, only update list every 2 minutes because github's list only updates every 5 minutes anyways
		if (config.watchListEndpoint().isEmpty() && lastSync.plus(120, ChronoUnit.SECONDS).isAfter(Instant.now()))
			return;
		// if not using default end point, update at the user configured cycle
		if (!config.watchListEndpoint().isEmpty() && lastSync.plus(config.syncLists(), ChronoUnit.SECONDS).isAfter(Instant.now()))
			return;

		caseManager.refresh(this::colorAll);
		lastSync = Instant.now();
	}
}
