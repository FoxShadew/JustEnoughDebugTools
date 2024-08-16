package dev.runefox.jedt.api.menu;

import net.minecraft.network.chat.Component;

/**
 * An option that opens a child menu, of type {@link ItemType#MENU}.
 *
 * @author Samū
 * @see MenuItem
 * @see AbstractItem
 * @since 0.1
 */
public class MenuOption extends AbstractItem {
    private final Menu menu;

    /**
     * @param name The name to display on the option widget
     * @param menu The menu to open
     * @since 0.1
     */
    public MenuOption(Component name, Menu menu) {
        super(name);
        this.menu = menu;
    }

    /**
     * @param menu The menu to open, of which the header is displayed on the option widget
     * @since 0.1
     */
    public MenuOption(Menu menu) {
        this(menu.getHeader(), menu);
    }

    @Override
    public final ItemType getType() {
        return ItemType.MENU;
    }

    @Override
    public void onClick(OptionSelectContext context) {
        context.openMenu(menu);
    }
}
