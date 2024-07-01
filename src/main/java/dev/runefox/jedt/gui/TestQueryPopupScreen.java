package dev.runefox.jedt.gui;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.runefox.jedt.gui.widgets.CompletableEditBox;
import dev.runefox.jedt.gui.widgets.RotationStepsSlider;
import dev.runefox.jedt.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.gametest.framework.TestClassNameArgument;
import net.minecraft.gametest.framework.TestFunctionArgument;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public class TestQueryPopupScreen extends VerticallyStackedScreen {
    private static final Component RUN_BUTTON_TEXT = Component.translatable("gui.jedt.test_query.run");

    private final Type type;
    private final BiConsumer<String, Integer> handler;
    private final CompletableEditBox.SuggestionsLayer suggestionsLayer = new CompletableEditBox.SuggestionsLayer(this);
    private CompletableEditBox<?> editBox;
    private Button runButton;
    private Button cancelButton;
    private RotationStepsSlider rotationSteps;
    private boolean initialized;

    public TestQueryPopupScreen(Screen parentScreen, Type type, BiConsumer<String, Integer> handler) {
        super(type.translation, parentScreen);
        this.type = type;
        this.handler = handler;

        if (type == Type.BATCH) {
            // TODO
            throw new UnsupportedOperationException("Not yet implemented");
        }

        minecraft = Minecraft.getInstance();
        font = minecraft.font;

        if (type.argumentType != null) {
            editBox = new CompletableEditBox<>(font, 0, 0, 200, 20, Component.nullToEmpty(""), suggestionsLayer, type.argumentType);
            addWidget(editBox);
        }

        if (type != Type.EXPORT_FUNCTION) {
            rotationSteps = new RotationStepsSlider(0, 0, 200, 20, 0);
            addWidget(rotationSteps);
        }

        runButton = GuiUtil.button(0, 0, 200, 20, RUN_BUTTON_TEXT, button -> {
            onClose();
            handler.accept(editBox == null ? "" : editBox.getValue(), rotationSteps == null ? 0 : rotationSteps.getRotationSteps());
        });
        addWidget(runButton);

        cancelButton = GuiUtil.button(0, 0, 200, 20, RUN_BUTTON_TEXT, button -> onClose());
        addWidget(cancelButton);
    }

    public Type getType() {
        return type;
    }

    @Override
    public void tick() {
        super.tick();
        if (runButton != null && editBox != null) {
            runButton.active = editBox.hasValidValue();
        }
    }

    @Override
    protected void init() {
        super.init();

        if (editBox != null) {
            ((ScreenAccessor) this).getChildren().add(suggestionsLayer);
            if (!initialized) {
                editBox.setFocused(true);
                setFocused(editBox);
            }
        }

        initialized = true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        renderBackground(graphics, mouseX, mouseY, tickProgress);
        super.render(graphics, mouseX, mouseY, tickProgress);

        graphics.drawCenteredString(font, getTitle(), width / 2, 20, 0xFFFFFFFF);

        if (editBox != null) {
            editBox.render(graphics, mouseX, mouseY, tickProgress);
            suggestionsLayer.render(graphics);
        }
    }

    @Override
    public void mouseMoved(double mx, double my) {
        suggestionsLayer.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double deltaX, double deltaY) {
        if (suggestionsLayer.isMouseOver(mx, my) && suggestionsLayer.mouseScrolled(mx, my, deltaX, deltaY))
            return true;
        return super.mouseScrolled(mx, my, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (suggestionsLayer.keyPressed(key, scan, mods))
            return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (suggestionsLayer.mouseClicked(mx, my, button))
            return true;
        return super.mouseClicked(mx, my, button);
    }

    public enum Type {
        FUNCTION("function", TestFunctionArgument.testFunctionArgument()),
        CLASS("class", TestClassNameArgument.testClassName()),
        ALL("all", null),
        EXPORT_FUNCTION("export_function", TestFunctionArgument.testFunctionArgument()),
        BATCH("batch", null);

        final String name;
        final Component translation;
        final ArgumentType<?> argumentType;

        Type(String name, ArgumentType<?> argumentType) {
            this.name = name;
            this.translation = Component.translatable("gui.debug.test_query." + name);
            this.argumentType = argumentType;
        }
    }
}
