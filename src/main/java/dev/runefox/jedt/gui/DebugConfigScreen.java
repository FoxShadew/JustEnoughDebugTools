package dev.runefox.jedt.gui;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import dev.runefox.jedt.DebugClient;
import dev.runefox.jedt.api.menu.ItemType;
import dev.runefox.jedt.api.menu.Menu;
import dev.runefox.jedt.api.menu.MenuItem;
import dev.runefox.jedt.api.menu.OptionSelectContext;
import dev.runefox.jedt.mixin.ScreenAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class DebugConfigScreen extends Screen {
    public static final DebugConfigScreen INSTANCE = new DebugConfigScreen();
    private static Menu root;

    private final List<ConfigMenu> menus = new ArrayList<>();
    private final List<HoverText> hoverTexts = new ArrayList<>();
    private final ClipboardManager clipboard = new ClipboardManager();
    private final DescriptionBox descriptionBox = new DescriptionBox();

    private boolean pauses;

    private int hoverItem;
    private int hoverMenu;
    private int focusItem;
    private int focusMenu;

    public DebugConfigScreen() {
        super(Component.translatable("jedt.options"));
    }

    @Override
    protected void init() {
        super.init();
        assert minecraft != null;

        if (!hasOpenMenus()) {
            openMenu(root, 0);

            children().clear();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return pauses;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        this.minecraft = minecraft;
        descriptionBox.resizeScreen(width, height);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        focusItem = -1;
        focusMenu = -1;
    }

    private void openMenu(Menu menu, int index) {
        ConfigMenu configMenu = new ConfigMenu(menu.getHeader());
        menu.options()
            .filter(MenuItem::isVisible)
            .map(opt -> entryFromOption(opt, index))
            .forEach(configMenu::addEntry);
        openMenu(configMenu, index);
    }

    private ConfigMenu.Entry entryFromOption(MenuItem option, int index) {
        ItemType type = option.getType();
        IntConsumer handler = incr -> {
            SelectionContext context = new SelectionContext(incr, index + 1);
            option.onClick(context);
        };

        return switch (type) {
            case ACTION ->
                new ConfigMenu.Entry(option, option.getName(), () -> handler.accept(0), option::getDisplayValue);
            case MENU ->
                new ConfigMenu.MenuEntry(option, option.getName(), () -> handler.accept(0), option::getDisplayValue);
            case BOOLEAN ->
                new ConfigMenu.CheckableEntry(option, option.getName(), () -> handler.accept(0), option::hasCheck, option::getDisplayValue);
            case NUMBER ->
                new ConfigMenu.SpinnerEntry(option, option.getName(), () -> handler.accept(Screen.hasShiftDown() ? -1 : 1), () -> handler.accept(1), () -> handler.accept(-1), option::getDisplayValue);
            case EXTERNAL ->
                new ConfigMenu.Entry(option, option.getName(), () -> handler.accept(0), option::getDisplayValue, 4);
        };
    }

    private void spawnHoverText(Component text) {
        assert minecraft != null;
        int mouseX = (int) (minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getHeight());

        int rx = (int) (Math.random() * 30 - 15);
        int ry = (int) (Math.random() * 30 - 15);

        HoverText txt = new HoverText(text, mouseX + rx, mouseY + ry);
        hoverTexts.add(txt);
    }

    private void closeMenusFrom(int index) {
        for (int i = index, s = menus.size(); i < s; i++) {
            menus.get(i).closeQuietly();
        }
    }

    private void openMenu(ConfigMenu menu, int index) {
        menu.open();
        if (index < menus.size()) {
            menu.forceVisible(false);
            ConfigMenu old = menus.set(index, menu);
            old.swapWith(menu);
        } else {
            menus.add(menu);
        }

        menu.setCloseHandler(() -> {
            if (index == 0) {
                onClose();
            }
            closeMenusFrom(index);
        });

        closeMenusFrom(index + 1);
    }

    private boolean isInvalidOverlayScreen(Screen screen) {
        if (screen == this) {
            return false;
        }

        if (screen instanceof DebugMenuOverlayScreen) {
            return ((DebugMenuOverlayScreen) screen).getParentScreen() != this;
        }

        return screen == null;
    }

    public void receiveTick() {
        // Close our menus if we're not literally overlayed
        if (minecraft == null || isInvalidOverlayScreen(minecraft.screen)) {
            for (ConfigMenu menu : menus) {
                menu.closeQuietly();
            }
        }
        menus.forEach(ConfigMenu::tick);
        menus.removeIf(ConfigMenu::isFullyClosed);

        children().clear();
        ((ScreenAccessor) this).getChildren().addAll(menus);

        hoverTexts.removeIf(txt -> txt.existTime-- < 0);
    }

    private int getTotalWidth(float partialTicks) {
        return menus.stream().mapToInt(menu -> menu.getDisplayableWidth(partialTicks)).sum();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        // The menu renders before this screen renders, via 'receiveRender' below.
        // If we call super, we call the blur effect which blurs our menu.
    }

    public void receiveRender(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        // Since we did not call 'init' yet our 'minecraft' field is null. We need it.
        minecraft = Minecraft.getInstance();

        RenderSystem.clear(GL32.GL_DEPTH_BUFFER_BIT);

        // Don't blur with no menus
//        if (hasOpenMenus())
//            renderBlurredBackground(tickProgress);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 110);
        RenderSystem.disableDepthTest();

        int totalWidth = getTotalWidth(tickProgress);

        int oversize = Math.max(0, totalWidth - width);
        int leftOffset = -oversize;

        boolean clearDescriptionBox = true;
        for (ConfigMenu menu : menus) {
            if (menu.canRender()) {
                int w = menu.getDisplayableWidth(tickProgress);
                menu.setHeight(height);
                menu.setLeftOffset(leftOffset);
                int lightUp = menu.hoveredIndex(mouseX, mouseY);
                if (menu.render(graphics, mouseX, mouseY, lightUp, tickProgress, descriptionBox)) {
                    clearDescriptionBox = false;
                }
                leftOffset += w;
            }
        }

        graphics.pose().translate(0, 0, 5);

        for (HoverText txt : hoverTexts) {
            float alpha = txt.existTime / 30f;
            int color = (int) (Mth.clamp(alpha, 0, 1) * 255) << 24;

            if ((color & 0xFC000000) != 0) {
                int width = minecraft.font.width(txt.text);
                drawHoverTextBackground(graphics, txt.x - width / 2 - 5, txt.y - 9, txt.x + width / 2 + 5, txt.y + 9, alpha);
                graphics.drawString(minecraft.font, txt.text, txt.x - width / 2, txt.y - 4, color | 0xFFFFFF);
            }
        }

        graphics.pose().translate(0, 0, 5);
        if (clearDescriptionBox) {
            descriptionBox.updateHovered(null, 0, 0, 0, 0, width, height);
        }
        descriptionBox.render(graphics, mouseX, mouseY, tickProgress);

        RenderSystem.enableDepthTest();
        graphics.pose().popPose();

    }

    public static void drawHoverTextBackground(GuiGraphics graphics, int x1, int y1, int x2, int y2, float alpha) {
        float bgAlpha = alpha / 2;
        int color = (int) (Mth.clamp(alpha, 0, 1) * 255) << 24;
        int lighterColor = color | 0xAAAAAA;
        int darkerColor = color | 0x777777;
        int bgColor = (int) (Mth.clamp(bgAlpha, 0, 1) * 255) << 24;

        RenderSystem.enableBlend();
        // Inner fill (transparent black)
        graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, bgColor);

        // Inner border (grey)
        graphics.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, lighterColor);
        graphics.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, darkerColor);
        graphics.fillGradient(x1 + 1, y1 + 2, x1 + 2, y2 - 2, lighterColor, darkerColor);
        graphics.fillGradient(x2 - 2, y1 + 2, x2 - 1, y2 - 2, lighterColor, darkerColor);

        // Outer border (black)
        graphics.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        graphics.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
        graphics.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        graphics.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    private void closeLastInteractiveMenu() {
        for (int i = menus.size() - 1; i >= 0; i--) {
            ConfigMenu menu = menus.get(i);
            if (menu.canInteract()) {
                menu.close();
                return;
            }
        }
    }

    private boolean hasOpenMenus() {
        for (ConfigMenu menu : menus) {
            if (!menu.isClosed()) {
                return true;
            }
        }
        return false;
    }

    private ConfigMenu menuOrNull(int index) {
        if (index < 0 || index >= menus.size())
            return null;

        return menus.get(index);
    }

    private ConfigMenu prepKeyboardInteraction() {
        ConfigMenu menu = menuOrNull(focusMenu);
        if (menu == null) {
            focusMenu = 0;
            closeMenusFrom(1);
            return menus.get(0);
        }
        return menu;
    }

    private void up() {
//        ConfigMenu menu = prepKeyboardInteraction();
//
//        focusItem --;
//        if (focusItem < 0)
//            focusItem = menu.size() - 1;
    }

    private void down() {
//        ConfigMenu menu = prepKeyboardInteraction();
//
//        focusItem ++;
//        if (focusItem >= menu.size())
//            focusItem = 0;
    }

    private void enter() {
//        if (focusMenu >= 0 && focusItem > 0) {
//            ConfigMenu menu = prepKeyboardInteraction();
//            ConfigMenu.Entry entry = menu.entry(focusItem);
//
//            if (entry instanceof )
//        }
    }

    private void left() {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeLastInteractiveMenu();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            for (ConfigMenu menu : menus) {
                menu.closeQuietly();
            }
            DebugClient.reloadMenus();
            openMenu(DebugClient.ROOT_MENU, 0);
            children().clear();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            enter();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            down();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            left();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            up();
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (DebugClient.debugOptionsKey.matches(keyCode, scanCode)) {
            DebugClient.f6KeyDown = true;
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        for (ConfigMenu menu : menus) {
            menu.closeQuietly();
        }
    }



    public static void show() {
        show(DebugClient.ROOT_MENU);
    }

    public static void show(Menu menu) {
        root = menu;
        Minecraft.getInstance().setScreen(INSTANCE);
    }

    private static class HoverText {
        private final Component text;
        private final int x, y;
        private int existTime = 30;

        private HoverText(Component text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private class SelectionContext implements OptionSelectContext {
        private final int increment;
        private final int index;

        private SelectionContext(int increment, int index) {
            this.increment = increment;
            this.index = index;
        }

        @Override
        public void spawnResponse(Component response) {
            spawnHoverText(response);
        }

        @Override
        public void openMenu(Menu menu) {
            DebugConfigScreen.this.openMenu(menu, index);
        }

        @Override
        public void copyToClipboard(String text) {
            assert minecraft != null;
            clipboard.setClipboard(minecraft.getWindow().getWindow(), text);
        }

        @Override
        public String getClipboard() {
            assert minecraft != null;
            return clipboard.getClipboard(minecraft.getWindow().getWindow(), null);
        }

        @Override
        public void closeScreen() {
            onClose();
        }

        @Override
        public void closeMenu() {
            closeMenusFrom(index);
        }

        @Override
        public int delta() {
            return increment;
        }

        @Override
        public Minecraft minecraft() {
            assert minecraft != null;
            return minecraft;
        }

        @Override
        public boolean screenPauses() {
            return pauses;
        }

        @Override
        public void setScreenPauses(boolean pause) {
            pauses = pause;
        }

        @Override
        public void openScreen(Screen screen) {
            minecraft().setScreen(screen);
        }

        @Override
        public Screen debugMenuScreen() {
            return DebugConfigScreen.this;
        }
    }
}
