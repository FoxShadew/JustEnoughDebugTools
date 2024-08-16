package dev.runefox.jedt.api.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A context passed into {@link MenuItem#onClick}. This can be used to obtain information about how the option was clicked,
 * or to trigger certain responses such as opening a child menu.
 *
 * @author Samū
 * @see MenuItem#onClick(OptionSelectContext)
 * @since 0.1
 */
public interface OptionSelectContext {

    /**
     * Spawns a hovering response tooltip near the mouse pointer.
     *
     * @param response The response text component. Must not be null.
     * @see #spawnResponse(String, Object...)
     * @since 0.1
     */
    void spawnResponse(Component response);

    /**
     * Opens a menu as a child menu of the menu this option is in. It is possible, but not recommended, to open a menu
     * that is already open.
     *
     * @param menu The menu to open. Must not be null.
     * @since 0.1
     */
    void openMenu(Menu menu);

    /**
     * Copies the given string to the clipboard.
     *
     * @param text The text to copy to the clipboard. Must not be null.
     * @since 0.1
     */
    void copyToClipboard(String text);

    /**
     * Returns the current string on the clipboard.
     *
     * @since 0.1
     */
    String getClipboard();

    /**
     * Closes the complete debug menu screen and brings focus back to the game. This can be interpreted as closing the
     * root menu.
     *
     * @since 0.1
     */
    void closeScreen();

    /**
     * Closes the menu the triggered option is in, and any child menus of that menu. When the triggered option is in the
     * root menu, the debug menu screen is closed as if {@link #closeScreen()} is called.
     *
     * @since 0.1
     */
    void closeMenu();

    /**
     * Spawns a hovering response tooltip near the mouse pointer.
     *
     * @param key    The translation key
     * @param values Any formatting values
     * @see #spawnResponse(Component)
     * @since 0.1
     */
    default void spawnResponse(String key, Object... values) {
        spawnResponse(Component.translatable(key, values));
    }

    /**
     * Returns whether reduced debug info is enabled.
     *
     * @since 0.1
     */
    default boolean reducedDebug() {
        return minecraft().showOnlyReducedInfo();
    }

    /**
     * Returns whether the current player has the given permission level.
     *
     * @param level The permission level
     * @since 0.1
     */
    default boolean hasPermissionLevel(int level) {
        Minecraft client = minecraft();
        assert client.player != null;
        return client.player.hasPermissions(level);
    }

    /**
     * Instantly execute a command as if it was entered in the chat box.
     *
     * @param command The command to execute. You may prefix this with a slash ({@code /}), but it's not required.
     * @since 0.1
     */
    default void sendCommand(String command) {
        // We'd generally allow a / in front of a command but the
        // server doesn't like this
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        Minecraft client = minecraft();
        assert client.player != null;
        client.player.connection.sendCommand(command);
    }

    // Number option context

    /**
     * Returns the amount of increment/decrement with which the value of a {@linkplain ItemType#NUMBER numeric option}
     * should be changed. When the option type is not numeric the returned value is 0.
     *
     * @since 0.1
     */
    int delta();

    /**
     * Shortcut to the {@link Minecraft} instance. This can also be obtained via {@link Minecraft#getInstance()}.
     *
     * @since 0.1
     */
    Minecraft minecraft();

    /**
     * Returns whether the debug menu screen pauses the game.
     *
     * @since 0.1
     */
    boolean screenPauses();

    /**
     * Sets whether the debug menu screen pauses the game.
     *
     * @since 0.1
     */
    void setScreenPauses(boolean pause);

    /**
     * Opens a screen.
     *
     * @param screen The screen to open
     * @since 0.2
     */
    void openScreen(Screen screen);

    /**
     * Returns the debug menu screen instance.
     *
     * @since 0.2
     */
    Screen debugMenuScreen();
}
