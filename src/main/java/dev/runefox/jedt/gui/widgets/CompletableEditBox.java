package dev.runefox.jedt.gui.widgets;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.runefox.jedt.gui.DebugConfigScreen;
import dev.runefox.jedt.mixin.EditBoxAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompletableEditBox<T> extends EditBox {
    private static final SimpleCommandExceptionType EXTRA_INPUT = new SimpleCommandExceptionType(
        Component.translatable("misc.jedt.extra_input_error")
    );

    private final SuggestionsLayer suggestionsLayer;
    private final ArgumentCommandNode<Object, T> type;
    private final Font font;
    private boolean bordered = true;
    private String error;
    private T value;
    private boolean empty = true;

    public CompletableEditBox(Font font, int x, int y, int width, int height, Component message, SuggestionsLayer suggestionsLayer, ArgumentType<T> type) {
        super(font, x, y, width, height, message);
        this.suggestionsLayer = suggestionsLayer;
        this.type = RequiredArgumentBuilder.argument("null", type).build();
        this.font = font;
    }

    public T getParsedValue() {
        return value;
    }

    public void setValue(T value, String text) {
        this.value = value;
        setValue(text);
    }

    public boolean hasValidValue() {
        error = null;
        setTextColor(0xFFFFFFFF);

        try {
            StringReader reader = new StringReader(getValue().trim());
            T value = type.getType().parse(reader);
            if (reader.canRead()) {
                throw EXTRA_INPUT.create();
            }
            this.value = value;
        } catch (CommandSyntaxException exc) {
            if (getValue().trim().isEmpty()) {
                error = null;
                empty = true;
            } else {
                error = exc.getRawMessage().getString();
                setTextColor(0xFFFF5555);
            }
        }
        return error == null && value != null && !empty;
    }

    protected Suggestions listSuggestions() {
        error = null;
        setTextColor(0xFFFFFFFF);

        SuggestionsBuilder builder = new SuggestionsBuilder(getTextUntilCursor(), 0);
        CompletableFuture<Suggestions> suggsFuture = type.getType().listSuggestions(createCommandContext(), builder);
        Suggestions suggs = suggsFuture.join();

        try {
            StringReader reader = new StringReader(getValue().trim());
            T value = type.getType().parse(reader);
            if (reader.canRead()) {
                throw EXTRA_INPUT.create();
            }
            this.value = value;
        } catch (CommandSyntaxException exc) {
            if (getValue().trim().isEmpty()) {
                error = null;
                empty = true;
            } else {
                error = exc.getRawMessage().getString();
                setTextColor(0xFFFF5555);
            }
        }

        return suggs;
    }

    private CommandContext<Object> createCommandContext() {
        String input = getTextUntilCursor();
        return new CommandContext<>(
            new Object(), input, Collections.emptyMap(),
            context -> 0, type, Collections.emptyList(),
            new StringRange(0, input.length()), null, null, false
        );
    }

    protected int compareSuggestions(String a, String b) {
        return a.compareTo(b);
    }

    private void attachAndUpdateSuggestions() {
        suggestionsLayer.attachTo(this);
        suggestionsLayer.updateSuggestions(listSuggestions());
    }

    @Override
    public void setFocused(boolean focus) {
        super.setFocused(focus);
        onFocusedChanged(focus);
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (super.charTyped(ch, mods)) {
            attachAndUpdateSuggestions();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (super.keyPressed(key, scancode, mods)) {
            attachAndUpdateSuggestions();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            attachAndUpdateSuggestions();
            return true;
        }
        return false;
    }

    @Override
    public void setValue(String text) {
        super.setValue(text);

        empty = text.trim().isEmpty();

        if (suggestionsLayer.owner == this || isFocused()) {
            attachAndUpdateSuggestions();
        }
    }

    protected void onFocusedChanged(boolean focus) {
        if (focus) {
            attachAndUpdateSuggestions();
        } else {
            suggestionsLayer.detachFrom(this);
        }
    }

    @Override
    public void setBordered(boolean border) {
        super.setBordered(border);
        bordered = border;
    }

    protected String getTextUntilCursor() {
        int cursor = getCursorPosition();

        return getValue().substring(0, cursor);
    }

    protected boolean canSuggest() {
        return getCursorPosition() == getValue().length();
    }

    protected int getStartX(StringRange range) {
        int lso = getLineScrollOffset();
        if (range.getStart() <= lso) {
            return 0;
        }

        String before = getValue().substring(lso, range.getStart());
        return font.width(before);
    }

    protected int getLineScrollOffset() {
        return ((EditBoxAccessor) this).debug_getDisplayPos();
    }

    protected void insertSuggestion(StringRange range, String text) {
        String before = getValue().substring(0, range.getStart());
        String after = getValue().substring(range.getEnd());
        int cursor = before.length() + text.length();
        setValue(before + text + after);
        setCursorPosition(cursor);
        setHighlightPos(cursor);
    }

    public interface ISuggestions {
        Suggestions listSuggestions(SuggestionsBuilder builder);
        void listSuggestions(String input, StringRange range, List<String> suggestions);
        default StringRange getSuggestableRange(String input) {
            return new StringRange(0, input.length());
        }
    }

    @SuppressWarnings("RegExpUnnecessaryNonCapturingGroup")
    public static class SuggestionsLayer implements Renderable, GuiEventListener {
        private static final int MAX_ELEMENTS_VISIBLE = 10;
        private static final Pattern WORD_BOUNDARY = Pattern.compile("\\b(?:$)");

        private final Screen parent;
        private final List<String> suggestions = new ArrayList<>();
        private int x;
        private int y;
        private int textX;
        private int width;
        private int selected;
        private int start;
        private int end;
        private boolean visible;
        private boolean up;
        private CompletableEditBox<?> owner;
        private StringRange suggestionRange;

        public SuggestionsLayer(Screen parent) {
            this.parent = parent;
        }

        private boolean isActive() {
            return owner != null && owner.visible
                       && (visible && !suggestions.isEmpty() || owner.error != null);
        }

        private List<String> trimError(String error, Font font) {
            List<String> out = new ArrayList<>();
            String e = error;
            while (!e.isEmpty() && out.size() < 5) {
                String trim = font.plainSubstrByWidth(e, width);
                if (trim.equals(e)) {
                    out.add(e.trim());
                    e = "";
                } else {
                    int index = -1;
                    Matcher matcher = WORD_BOUNDARY.matcher(trim);
                    while (matcher.find()) {
                        index = matcher.start();
                    }

                    if (index == -1) {
                        index = trim.length();
                    }
                    out.add(e.substring(0, index).trim());
                    e = e.substring(index);
                }
            }
            return out;
        }

        public void render(GuiGraphics graphics) {
            if (isActive()) {
                graphics.flush();

                Font font = owner.font;


                String error = owner.error;

                if (error != null) {
                    List<String> errorLines = trimError(error, font);
                    int height = errorLines.size() * 10 + 2;
                    int cy = up ? owner.getY() + owner.height : owner.getY() - height;

                    // Background
                    graphics.fill(x - 1, cy, x + width + 1, cy + height, 0xAA000000);

                    // Errors
                    int textY = cy + 1;
                    for (String errLn : errorLines) {
                        graphics.drawString(font, errLn, x, textY + 2, 0xFFFF5555);

                        textY += 10;
                    }
                }

                if (!suggestions.isEmpty()) {
                    int height = (end - start) * 12 + 2;
                    int y = effY(height);

                    // Background
                    DebugConfigScreen.drawHoverTextBackground(
                        graphics,
                        x - 3, y, x + width + 3, y + height,
                        1
                    );

                    // Text
                    int textY = y + 1;
                    for (int i = start; i < end; i++) {
                        if (i >= 0 && i < suggestions.size()) {
                            String sugg = suggestions.get(i);
                            int color = i == selected ? 0xFFFFFF55 : 0xFFFFFFFF;

                            graphics.drawString(font, sugg, x + textX, textY + 2, color);
                        }

                        textY += 12;
                    }

                    // Dotted lines at top and bottom, when scrolling is possible
                    boolean upperLine = start != 0;
                    boolean lowerLine = end != suggestions.size();
                    for (int ix = 0; ix < width; ix++) {
                        if ((ix & 1) == 0) {
                            if (upperLine) {
                                graphics.fill(x + ix, y, x + ix + 1, y + 1, 0xFFFFFFFF);
                            }
                            if (lowerLine) {
                                graphics.fill(x + ix, y + height - 1, x + ix + 1, y + height, 0xFFFFFFFF);
                            }
                        }
                    }
                }

                graphics.flush();
            }
        }

        private int effY(int height) {
            return up ? y - height : y;
        }

        private boolean isInside(double mx, double my) {
            int height = (end - start) * 12 + 2;
            int y = effY(height);
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }

        private boolean isInside(double mx, double my, int y1, int y2) {
            return mx >= x && mx <= x + width && my >= y1 && my <= y2;
        }

        protected void setSelected(int selected) {
            if (suggestions.isEmpty()) {
                this.selected = 0;
                owner.setSuggestion(null);
                return;
            }
            int s = suggestions.size();
            selected %= s;
            if (selected < 0) selected += s;

            this.selected = selected;
            ensureIndexIsVisible(selected);

            if (owner != null) {
                String text = selectedText();
                String ownerText = suggestionRange.get(owner.getValue());
                if (text != null
                        && owner.canSuggest()
                        && text.length() > ownerText.length()
                        && text.toLowerCase().startsWith(ownerText.toLowerCase())
                ) {
                    owner.setSuggestion(text.substring(ownerText.length()));
                } else {
                    owner.setSuggestion(null);
                }
            }
        }

        protected void setEnd(int end) {
            int diff = this.end - start;
            this.end = end;
            this.start = end - diff;
        }

        protected void setStart(int start) {
            int diff = end - this.start;
            this.start = start;
            this.end = start + diff;
        }

        private void ensureIndexIsVisible(int index) {
            if (index < start) setStart(index);
            if (index >= end) setEnd(index + 1);
        }

        private void updateStartAndEnd() {
            int elems = suggestions.size();

            start = 0;
            end = Math.min(elems, MAX_ELEMENTS_VISIBLE);
        }

        protected String selectedText() {
            if (selected < 0 || selected >= suggestions.size()) return null;
            return suggestions.get(selected);
        }

        protected int selectedIndex() {
            return selected;
        }

        protected void updateSuggestions(Suggestions suggs) {
            String selected = selectedText();
            StringRange lastRange = suggestionRange;

            suggestions.clear();
            suggestionRange = suggs.getRange();

            for (Suggestion sugg : suggs.getList()) {
                suggestions.add(sugg.getText());
            }

            updateStartAndEnd();

            if (suggestions.size() == 0) {
                this.selected = 0;
                owner.setSuggestion(null);
                return;
            }

            if (suggestionRange.equals(lastRange)) {
                int selIndex = suggestions.indexOf(selected);
                if (selIndex == -1) {
                    selIndex = 0;
                }
                setSelected(selIndex);
            } else {
                setSelected(0);
            }

            textX = owner.getStartX(suggestionRange);
        }

        public void attachTo(CompletableEditBox<?> owner) {
            this.owner = owner;

            up = false;
            int cy = owner.getY() + owner.height;
            int height = MAX_ELEMENTS_VISIBLE * 12 + 2;
            if (cy + height > parent.height) {
                cy = owner.getY();
                up = true;
            }

            x = owner.getX() + (owner.bordered ? 4 : 0);
            y = cy;
            width = owner.width - (owner.bordered ? 8 : 0);
            visible = true;
        }

        public void detachFrom(CompletableEditBox<?> owner) {
            if (this.owner == owner) {
                visible = false;
                owner.setSuggestion(null);
                this.owner = null;
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (isActive()) {
                if (scrollY < -1) scrollY = -1;
                if (scrollY > 1) scrollY = 1;
                int delta = (int) scrollY;

                int diff = end - start;
                int newStart = start - delta;
                int newEnd = end - delta;
                if (newEnd > suggestions.size()) {
                    newEnd = suggestions.size();
                    newStart = newEnd - diff;
                }
                if (newStart < 0) {
                    newStart = 0;
                    newEnd = diff;
                }
                start = newStart;
                end = newEnd;

                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(int key, int scancode, int mods) {
            if (isActive()) {
                if (key == GLFW.GLFW_KEY_UP) {
                    setSelected(selected - 1);
                    return true;
                } else if (key == GLFW.GLFW_KEY_DOWN) {
                    setSelected(selected + 1);
                    return true;
                } else if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    String sel = selectedText();
                    if (sel != null) {
                        owner.insertSuggestion(suggestionRange, sel);
                        owner.setSuggestion(null);
                        visible = false;
                        return true;
                    }
                } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                    owner.setSuggestion(null);
                    visible = false;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            if (isActive()) {
                if (isInside(mouseX, mouseY)) {
                    int height = (end - start) * 12 + 2;
                    int textY = effY(height);

                    for (int i = start; i < end; i++) {
                        if (isInside(mouseX, mouseY, textY, textY + 12)) {
                            setSelected(i);
                            break;
                        }

                        textY += 12;
                    }
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isActive()) {
                if (isInside(mouseX, mouseY)) {
                    int height = (end - start) * 12 + 2;
                    int textY = effY(height);

                    for (int i = start; i < end; i++) {
                        if (isInside(mouseX, mouseY, textY, textY + 12)) {
                            String sel = i < 0 || i >= suggestions.size() ? null : suggestions.get(i);
                            if (sel != null) {
                                owner.insertSuggestion(suggestionRange, sel);
                                owner.setSuggestion(null);
                                owner.setFocused(true);
                                parent.setFocused(owner);
                                visible = false;
                                return true;
                            }
                        }

                        textY += 12;
                    }
                    return false;
                } else {
                    visible = false;
                }
            }
            return false;
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            int height = (end - start) * 12 + 2;
            double rx = mx - x;
            double ry = my - y;
            return visible && rx >= 0 && rx <= width && ry >= 0 && ry <= height;
        }

        @Override
        public void setFocused(boolean focused) {

        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
            render(guiGraphics);
        }
    }
}
