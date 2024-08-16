package dev.runefox.jedt.api.menu;

import net.minecraft.network.chat.Component;

/**
 * An abstract clickable option, of type {@link ItemType#EXTERNAL}.
 *
 * @author Samū
 * @see MenuItem
 * @see AbstractItem
 * @since 0.2
 */
public abstract class ExternalItem extends AbstractItem {

    /**
     * @param name The name to display on the option widget
     * @since 0.2
     */
    public ExternalItem(Component name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ItemType#EXTERNAL}
     *
     * @since 0.2
     */
    @Override
    public final ItemType getType() {
        return ItemType.EXTERNAL;
    }
}
