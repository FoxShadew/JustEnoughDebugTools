package net.shadew.debug.impl.menu;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

import net.shadew.debug.api.menu.Menu;
import net.shadew.debug.api.menu.MenuManager;

public class MenuManagerImpl implements MenuManager {
    public static final ResourceLocation ROOT = new ResourceLocation("jedt:root");

    private final HashMap<ResourceLocation, MenuImpl> menuInstances = new HashMap<>();

    @Override
    public Menu getMenu(ResourceLocation name) {
        if (name == null) {
            name = ROOT;
        }
        return menuInstances.computeIfAbsent(name, this::createMenu);
    }

    public void clearAll() {
        menuInstances.forEach((key, menu) -> menu.clear());
    }

    public Map<ResourceLocation, MenuImpl> getAllMenus() {
        return menuInstances;
    }

    private MenuImpl createMenu(ResourceLocation name) {
        return new MenuImpl(Component.translatable(Util.makeDescriptionId("debug.menu", name)));
    }
}
