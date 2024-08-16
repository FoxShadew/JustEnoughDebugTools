package dev.runefox.jedt.impl.menu;

import net.minecraft.network.chat.Component;

import dev.runefox.jedt.api.menu.Menu;
import dev.runefox.jedt.api.menu.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MenuImpl implements Menu {
    private final Component header;
    private final List<MenuItem> options = new ArrayList<>();

    public MenuImpl(Component header) {
        this.header = header;
    }

    @Override
    public Component getHeader() {
        return header;
    }

    @Override
    public Stream<MenuItem> options() {
        return options.stream();
    }

    @Override
    public void addOption(MenuItem option) {
        options.add(option);
    }

    public void clear() {
        options.clear();
    }
}
