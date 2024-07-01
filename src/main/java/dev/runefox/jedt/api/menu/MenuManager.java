package dev.runefox.jedt.api.menu;

import dev.runefox.jedt.api.MenuInitializer;
import net.minecraft.resources.ResourceLocation;

/**
 * A manager for {@link Menu} instances. {@link Menu} instances can only be obtained via an instance of this interface.
 * A {@link MenuManager} instance is passed into each {@link MenuInitializer}. For compatibility with other mods using
 * this mod and interaction with the builtin menus, menus are all identified by a {@link ResourceLocation}. The
 * identifiers of the default menus are specified as constants in the {@link Menu} interface.
 *
 * @author Samū
 * @see Menu
 * @see MenuInitializer
 * @since 0.1
 */
public interface MenuManager {
    /**
     * Gets or creates a {@link Menu} with the given identifier. The identifier is parsed as a {@link ResourceLocation}
     * and immediately redirected to {@link #getMenu(ResourceLocation)}.
     *
     * @param name The menu identifier, or null to get the root ({@code debug:root}) menu
     * @return The obtained {@link Menu} instance
     *
     * @since 0.1
     */
    default Menu getMenu(String name) {
        return getMenu(name == null ? null : ResourceLocation.parse(name));
    }

    /**
     * Gets or creates a {@link Menu} with the given identifier.
     *
     * @param name The menu identifier, or null to get the root ({@code debug:root}) menu
     * @return The obtained {@link Menu} instance
     *
     * @since 0.1
     */
    Menu getMenu(ResourceLocation name);
}
