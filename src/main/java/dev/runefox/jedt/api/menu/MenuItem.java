package dev.runefox.jedt.api.menu;

import net.minecraft.network.chat.Component;

/**
 * An option to add to a menu. You may implement this interface to create your custom options, but you likely want to
 * use or inherit one of the default implementations.
 *
 * @author Samū
 * @see Menu
 * @see AbstractItem
 * @see ActionItem
 * @see BooleanItem
 * @see NumberItem
 * @see SimpleActionItem
 * @see CommandItem
 * @see MenuOption
 * @since 0.1
 */
public interface MenuItem {

    /**
     * Returns the component to display on the widget of this debug option. Must not return null.
     *
     * @since 0.1
     */
    Component getName();

    /**
     * Returns a longer name to display on the widget to show when it is hovered. This is intended for options with long
     * names that do not fit on the option widget. When null is returned, no long name will be displayed.
     *
     * @since 0.2
     */
    default Component getLongName() {
        return null;
    }

    /**
     * Returns the type of this option. See {@link ItemType} for a set of valid option types. This determines the
     * appearance of the option in the menu. Must not return null.
     *
     * @since 0.1
     */
    ItemType getType();

    /**
     * Triggered as soon as the option is clicked in the debug menu. The given {@link OptionSelectContext} instance
     * contains some extra information on where and how the option was clicked.
     *
     * @param context The {@link OptionSelectContext} instance
     */
    void onClick(OptionSelectContext context);

    /**
     * Returns an informative description for this option. When null is returned, this option will have no description.
     *
     * @since 0.2
     */
    default Component getDescription() {
        return null;
    }

    /**
     * Used when the option type returned by {@link #getType} is {@link ItemType#BOOLEAN} to determine whether the
     * option is checked. It is important that the option itself does not manage the checked state, this should read the
     * state from an external container, or return true or false constantly.
     *
     * @return True when a check should be displayed, false when not.
     *
     * @since 0.1
     */
    default boolean hasCheck() {
        return false;
    }

    /**
     * Returns a value to display on the right side of the option widget. This is, for example, used in numeric options
     * to display the numeric value, but any option can have a value set here. When null is returned, no value is
     * displayed.
     *
     * @since 0.1
     */
    default Component getDisplayValue() {
        return null;
    }

    /**
     * Returns whether this option is visible. This is called at the moment the container menu is opened and can, for
     * example, check whether reduced debug info is disabled. By default, this method returns true.
     */
    default boolean isVisible() {
        return true;
    }
}
