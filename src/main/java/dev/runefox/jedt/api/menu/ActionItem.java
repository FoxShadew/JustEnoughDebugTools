package dev.runefox.jedt.api.menu;

import net.minecraft.network.chat.Component;

/**
 * An abstract clickable option, of type {@link ItemType#ACTION}.
 *
 * @author Samū
 * @see MenuItem
 * @see AbstractItem
 * @since 0.1
 */
public abstract class ActionItem extends AbstractItem {

    /**
     * @param name The name to display on the option widget
     * @since 0.1
     */
    public ActionItem(Component name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ItemType#ACTION}
     *
     * @since 0.1
     */
    @Override
    public final ItemType getType() {
        return ItemType.ACTION;
    }
}
