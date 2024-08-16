package dev.runefox.jedt.api.menu;

/**
 * The types of debug menu options. This is returned by {@link MenuItem#getType()} to determine the appearance and behavior
 * of the option.
 *
 * @author Samū
 * @see MenuItem#getType()
 * @since 0.1
 */
public enum ItemType {
    /**
     * Type for a basic button option. This renders the option as a plain button.
     *
     * @since 0.1
     */
    ACTION,

    /**
     * Type for an option with that opens a submenu. This renders the option as a button with a right-pointing arrow.
     *
     * @since 0.1
     */
    MENU,

    /**
     * Type for an option that can be toggled. This renders a check on the option if enabled.
     *
     * @since 0.1
     */
    BOOLEAN,

    /**
     * Type for an option with a numeric or spinnable value. This renders two arrow buttons on the option.
     *
     * @since 0.1
     */
    NUMBER,

    /**
     * Type for an option that opens another screen or pop-up. This renders a diagonal arrow on the option.
     *
     * @since 0.2
     */
    EXTERNAL
}
