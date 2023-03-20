package com.DMWatch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.inject.Provides;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;


@Slf4j
@PluginDescriptor(
        name = "DMWatch",
        description = "",
        tags = {},
        enabledByDefault = false
)
public class DMWatchPlugin extends Plugin {
    private static final String INVESTIGATE = "Investigate FW";
    private static final String NBSP = Character.toString((char) 160);

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

    private enum AlertType {
        NONE(""),
        FRIENDS_CHAT("Friends chat member, "),
        CLAN_CHAT("Clan chat member, "),
        NEARBY("Nearby player, ");

        private final String message;

        AlertType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

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

    @Inject
    private DMWatchOverlay screenshotOverlay;

    @Getter(AccessLevel.PACKAGE)
    private BufferedImage reportButton;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    CaseManager caseManager;

    @Inject
    ClientThread clientThread;

    @Inject
    @Named("developerMode")
    boolean developerMode;

    @Inject
    DMWatchInputListener hotkeyListener;

    @Inject
    KeyManager keyManager;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private boolean hotKeyPressed;

    @Provides
    DMWatchConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DMWatchConfig.class);
    }

    @Override
    protected void startUp() {
        if (config.playerOption() && client != null) {
            menuManager.addPlayerMenuItem(INVESTIGATE);
        }

        keyManager.registerKeyListener(hotkeyListener);
        spriteManager.getSpriteAsync(SpriteID.CHATBOX_REPORT_BUTTON, 0, s -> reportButton = s);
        overlayManager.add(screenshotOverlay);
        caseManager.refresh(this::colorAll);
    }

    @Override
    protected void shutDown() {
        if (config.playerOption() && client != null) {
            menuManager.removePlayerMenuItem(INVESTIGATE);
        }
        keyManager.unregisterKeyListener(hotkeyListener);
        overlayManager.remove(screenshotOverlay);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(DMWatchConfig.CONFIG_GROUP)) {
            return;
        }

        if (event.getKey().equals(DMWatchConfig.PLAYER_OPTION)) {
            if (!Boolean.parseBoolean(event.getOldValue()) && Boolean.parseBoolean(event.getNewValue())) {
                menuManager.addPlayerMenuItem(INVESTIGATE);
            } else if (Boolean.parseBoolean(event.getOldValue()) && !Boolean.parseBoolean(event.getNewValue())) {
                menuManager.removePlayerMenuItem(INVESTIGATE);
            }
        } else if (event.getKey().equals(DMWatchConfig.PLAYER_TEXT_COLOR)) {
            colorAll();
        }
    }

    @Subscribe
    public void onFocusChanged(FocusChanged focusChanged) {
        if (!focusChanged.isFocused()) {
            hotKeyPressed = false;
        }
    }

    private void colorAll() {
        clientThread.invokeLater(() -> {
            colorFriendsChat();
            colorClanChat();
            colorGuestClanChat();
        });
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned playerSpawned) {
        if (!config.notifyOnNearby()) {
            return;
        }
        String name = playerSpawned.getPlayer().getName();
        this.alertPlayerWarning(name, false, AlertType.NEARBY);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!config.menuOption() || (!hotKeyPressed && config.useHotkey())) {
            return;
        }

        int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
        String option = event.getOption();

        if (!MENU_WIDGET_IDS.contains(groupId) || !AFTER_OPTIONS.contains(option)) {
            return;
        }

        for (MenuEntry me : client.getMenuEntries()) {
            // don't add menu option if we've already added investigate
            if (INVESTIGATE.equals(me.getOption())) {
                return;
            }
        }


        client.createMenuEntry(-1)
                .setOption(INVESTIGATE)
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier());
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        Runnable color = null;
        switch (event.getScriptId()) {
            case ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD:
                color = this::colorFriendsChat;
                break;
        }

        if (color != null) {
            clientThread.invokeLater(color);
        }
    }

    @Subscribe
    public void onFriendsChatMemberJoined(FriendsChatMemberJoined event) {
        if (!config.notifyOnJoin()) {
            return;
        }
        String rsn = Text.toJagexName(event.getMember().getName());
        String local = client.getLocalPlayer().getName();
        if (rsn.equals(local)) {
            return;
        }

        alertPlayerWarning(rsn, false, AlertType.FRIENDS_CHAT);
    }

    @Subscribe
    public void onClanMemberJoined(ClanMemberJoined event) {
        if (!config.notifyOnJoin()) {
            return;
        }
        String rsn = Text.toJagexName(event.getClanMember().getName());
        String local = client.getLocalPlayer().getName();
        if (rsn.equals(local)) {
            return;
        }

        alertPlayerWarning(rsn, false, AlertType.CLAN_CHAT);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        int groupId = WidgetInfo.TO_GROUP(event.getWidgetId());
        String option = event.getMenuOption();
        MenuAction action = event.getMenuAction();

        // https://github.com/runelite/runelite/blob/f6c68eefc8a3c5415451bdbd6190b1f745ed5489/runelite-client/src/main/java/net/runelite/client/plugins/hiscore/HiscorePlugin.java#L178
        if ((action == MenuAction.RUNELITE || action == MenuAction.RUNELITE_PLAYER) && option.equals(INVESTIGATE)) {
            final String target;
            if (action == MenuAction.RUNELITE_PLAYER) {
                // The player id is included in the event, so we can use that to get the player name,
                // which avoids having to parse out the combat level and any icons preceding the name.
                Player player = client.getCachedPlayers()[event.getId()];
                if (player != null) {
                    target = player.getName();
                } else {
                    target = null;
                }
            } else {
                target = Text.removeTags(event.getMenuTarget());
            }

            if (target != null) {
                caseManager.get(event.getMenuTarget(), (rwCase) -> alertPlayerWarning(target, true, AlertType.NONE));
            }
        }

    }

    @Schedule(period = 15, unit = ChronoUnit.MINUTES)
    public void refreshList() {
        caseManager.refresh(this::colorAll);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted ce) {
        if (developerMode && ce.getCommand().equals("rwd")) {
            caseManager.refresh(() -> {
                if (ce.getArguments().length > 0) {
                    // refresh is async, so wait a bit before adding the test rsn
                    caseManager.put(
                            String.join(" ", Arrays.copyOfRange(ce.getArguments(), 1, ce.getArguments().length)),
                            ce.getArguments()[0].toUpperCase()
                    );
                }
                colorAll();
            });
        } else if (ce.getCommand().equals("rw")) {
            final String rsn = String.join(" ", ce.getArguments());
            caseManager.get(rsn, (c) -> alertPlayerWarning(rsn, true, AlertType.NONE));
        }
    }

    private void alertPlayerWarning(String rsn, boolean notifyClear, AlertType alertType) {
        rsn = Text.toJagexName(rsn);
        Case rwCase = caseManager.get(rsn);
        ChatMessageBuilder response = new ChatMessageBuilder();
        response.append(alertType.getMessage())
                .append(ChatColorType.HIGHLIGHT)
                .append(rsn)
                .append(ChatColorType.NORMAL);

        if (rwCase == null && !notifyClear) {
            return;
        } else if (rwCase == null) {
            response.append(" is not on any watchlist.");
        } else {
            response.append(String.format(" is on %s list for ", rwCase.niceSourcePossessive()))
                    .append(ChatColorType.HIGHLIGHT)
                    .append(rwCase.getReason());
            if (rwCase.getDate().getTime() > 0) {
                response.append(" ")
                        .append(ChatColorType.NORMAL)
                        .append("on " + rwCase.niceDate())
                        .append(".");
            } else {
                response.append(ChatColorType.NORMAL)
                        .append(".");
            }
        }

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(response.build())
                .build());
    }

    private void colorFriendsChat() {
        Widget ccList = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);
        if (ccList != null) {
            illiteratePlayerWidgets(ccList);
        }
    }

    private void colorClanChat() {
        Widget clanChatList = client.getWidget(WidgetInfo.CLAN_MEMBER_LIST);
        if (clanChatList != null) {
            illiteratePlayerWidgets(clanChatList);
        }
    }

    private void colorGuestClanChat() {
        Widget guestClanChatList = client.getWidget(WidgetInfo.CLAN_MEMBER_LIST);
        if (guestClanChatList != null) {
            illiteratePlayerWidgets(guestClanChatList);
        }
    }

    private void illiteratePlayerWidgets(Widget chatWidget) {
        Widget[] players = chatWidget.getDynamicChildren();
        for (int i = 0; i < players.length; i += 3) {
            Widget player = players[i];
            if (player == null) {
                continue;
            }

            Case rwCase = caseManager.get(player.getText());
            if (rwCase == null) {
                continue;
            }

            player.setTextColor(config.playerTextColor().getRGB());
            player.revalidate();
        }
    }


}
