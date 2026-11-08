package sk.breezelook.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sk.breezelook.Main;
import sk.breezelook.Main.LookDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Point editor: a list of existing points on the left, and a form on the right with the name,
 * the two angles and an icon picked from block images.
 */
public class WarpSettingsScreen extends Screen {
    private static final int CELL = 20;
    private static final int ROW = 22;
    private static final int SELECTED = 0xFF3A7BD5;

    private final List<String> names = new ArrayList<>();
    private List<Item> filtered = List.of();

    /** Name of the point being edited; null while creating a new one. */
    private String editing;
    private String selectedIcon;
    private String nameValue = "";
    private String horizontalValue = "0";
    private String verticalValue = "0";
    private String searchValue = "";
    private Component status = Component.empty();
    private boolean statusError;

    private int listScroll;
    private int gridScroll;

    private EditBox nameBox;
    private EditBox horizontalBox;
    private EditBox verticalBox;
    private EditBox searchBox;
    private Button saveButton;
    private Button deleteButton;

    private int listX, listY, listW, listH;
    private int formX, formW;
    private int iconLabelY;
    private int gridX, gridY, gridCols, gridRows;
    private int buttonsY;

    public WarpSettingsScreen() {
        super(Component.translatable("breezelook.settings.title"));
    }

    @Override
    protected void init() {
        refreshNames();

        int panelWidth = Math.min(width - 16, 400);
        int left = (width - panelWidth) / 2;
        listX = left;
        listY = 28;
        listW = 112;
        listH = Math.max(40, height - listY - 12);
        formX = left + listW + 8;
        formW = panelWidth - listW - 8;

        int y = 28;
        nameBox = addBox(formX, y + 10, formW);
        nameBox.setMaxLength(32);
        y += 34;

        int half = (formW - 4) / 2;
        horizontalBox = addBox(formX, y + 10, half);
        verticalBox = addBox(formX + half + 4, y + 10, formW - half - 4);
        y += 34;

        iconLabelY = y;
        // the selected-icon preview sits on the label row (16px tall), so keep the search box below it
        searchBox = addBox(formX, y + 20, formW);
        searchBox.setHint(Component.translatable("breezelook.settings.search"));
        y += 42;

        gridX = formX;
        gridY = y;
        gridCols = Math.max(1, (formW - 6) / CELL);
        buttonsY = height - 28;
        gridRows = Math.max(1, Math.min(8, (buttonsY - 14 - gridY) / CELL));

        nameBox.setResponder(value -> nameValue = value);
        horizontalBox.setResponder(value -> horizontalValue = value);
        verticalBox.setResponder(value -> verticalValue = value);
        searchBox.setResponder(value -> {
            searchValue = value;
            gridScroll = 0;
            filtered = WarpIcons.search(value);
        });
        nameBox.setValue(nameValue);
        horizontalBox.setValue(horizontalValue);
        verticalBox.setValue(verticalValue);
        searchBox.setValue(searchValue);
        filtered = WarpIcons.search(searchValue);

        int buttonWidth = (formW - 8) / 3;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("breezelook.settings.add"), button -> save())
                .bounds(formX, buttonsY, buttonWidth, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("breezelook.settings.delete"), button -> delete())
                .bounds(formX + buttonWidth + 4, buttonsY, buttonWidth, 20).build());
        int doneX = formX + (buttonWidth + 4) * 2;
        addRenderableWidget(Button.builder(Component.translatable("breezelook.settings.done"), button -> onClose())
                .bounds(doneX, buttonsY, formX + formW - doneX, 20).build());

        // The warp list and the icon grid are real widgets, so clicks and scrolling reach them
        // through the normal "child under the cursor" routing, like the buttons above.
        addRenderableWidget(new AreaWidget(listX, listY, listW, listH,
                this::drawList, this::clickList, this::scrollList));
        addRenderableWidget(new AreaWidget(gridX, gridY, gridCols * CELL, gridRows * CELL,
                this::drawGrid, this::clickGrid, this::scrollGrid));
        updateButtons();
    }

    private EditBox addBox(int x, int y, int w) {
        return addRenderableWidget(new EditBox(font, x, y, w, 18, Component.empty()));
    }

    private void refreshNames() {
        names.clear();
        names.addAll(Main.points.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
    }

    private void updateButtons() {
        if (saveButton == null) return;
        saveButton.setMessage(Component.translatable(editing == null ? "breezelook.settings.add" : "breezelook.settings.update"));
        deleteButton.active = editing != null;
    }

    private void setStatus(String key, boolean error) {
        status = Component.translatable(key);
        statusError = error;
    }

    // ---------------------------------------------------------------- actions

    private void newPoint() {
        editing = null;
        selectedIcon = null;
        status = Component.empty();
        nameBox.setValue("");
        horizontalBox.setValue("0");
        verticalBox.setValue("0");
        updateButtons();
    }

    private void loadPoint(String name) {
        LookDirection d = Main.points.get(name);
        if (d == null) return;
        editing = name;
        selectedIcon = d.icon();
        status = Component.empty();
        nameBox.setValue(name);
        horizontalBox.setValue(format(d.horizontal()));
        verticalBox.setValue(format(d.vertical()));
        updateButtons();
    }

    private void save() {
        String name = nameValue.trim().replaceAll("\\s+", "_");
        if (name.isEmpty()) {
            setStatus("breezelook.settings.error.name", true);
            return;
        }
        float horizontal;
        float vertical;
        try {
            horizontal = parse(horizontalValue);
            vertical = parse(verticalValue);
        } catch (NumberFormatException e) {
            setStatus("breezelook.settings.error.angle", true);
            return;
        }
        boolean renamed = editing != null && !editing.equals(name);
        if ((editing == null || renamed) && Main.points.containsKey(name)) {
            setStatus("breezelook.settings.error.exists", true);
            return;
        }

        LookDirection old = editing != null ? Main.points.get(editing) : null;
        float chamberHorizontal = old != null ? old.chamberHorizontal() : 0;
        float chamberVertical = old != null ? old.chamberVertical() : 0;
        if (renamed) Main.points.remove(editing);
        Main.points.put(name, new LookDirection(horizontal, vertical, chamberHorizontal, chamberVertical, selectedIcon));
        Main.savePoints();

        editing = name;
        refreshNames();
        updateButtons();
        setStatus("breezelook.settings.saved", false);
    }

    private void delete() {
        if (editing == null) return;
        Main.points.remove(editing);
        Main.savePoints();
        refreshNames();
        listScroll = Math.min(listScroll, Math.max(0, names.size() + 1 - visibleRows()));
        newPoint();
        setStatus("breezelook.settings.deleted", false);
    }

    private static float parse(String text) {
        float value = Float.parseFloat(text.trim().replace(',', '.'));
        if (!Float.isFinite(value)) throw new NumberFormatException("not finite");
        return value;
    }

    private static String format(float value) {
        String text = Float.toString(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    // ---------------------------------------------------------------- layout helpers

    private int visibleRows() {
        return Math.max(1, listH / ROW);
    }

    private int totalGridRows() {
        return (filtered.size() + gridCols - 1) / gridCols;
    }

    private int maxGridScroll() {
        return Math.max(0, totalGridRows() - gridRows);
    }

    /** Index into {@link #filtered} of the cell under the cursor, or -1. */
    private int gridIndexAt(double mx, double my) {
        if (mx < gridX || mx >= gridX + gridCols * CELL || my < gridY || my >= gridY + gridRows * CELL) return -1;
        int col = (int) ((mx - gridX) / CELL);
        int row = (int) ((my - gridY) / CELL);
        int index = (gridScroll + row) * gridCols + col;
        return index < filtered.size() ? index : -1;
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        // drawn in extractRenderState so panels end up below the widgets
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        extractTransparentBackground(g);
        g.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);

        g.text(font, Component.translatable("breezelook.settings.name"), formX, nameBox.getY() - 10, 0xFFA0A0A0);
        g.text(font, Component.translatable("breezelook.settings.horizontal"), horizontalBox.getX(), horizontalBox.getY() - 10, 0xFFA0A0A0);
        g.text(font, Component.translatable("breezelook.settings.vertical"), verticalBox.getX(), verticalBox.getY() - 10, 0xFFA0A0A0);
        g.text(font, Component.translatable("breezelook.settings.icon"), formX, iconLabelY, 0xFFA0A0A0);
        g.item(WarpIcons.stackFor(selectedIcon), formX + formW - 16, iconLabelY - 3);

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) renderable.extractRenderState(g, mx, my, partialTick);
        }

        if (!status.getString().isEmpty()) {
            g.text(font, status, formX, buttonsY - 11, statusError ? 0xFFFF5555 : 0xFF55FF55);
        }
    }

    private void drawList(GuiGraphicsExtractor g, int mx, int my) {
        g.fill(listX, listY, listX + listW, listY + listH, 0x90000000);
        int rows = visibleRows();
        int total = names.size() + 1;
        for (int r = 0; r < rows; r++) {
            int row = listScroll + r;
            if (row >= total) break;
            int y = listY + r * ROW;
            boolean isNew = row == 0;
            String name = isNew ? null : names.get(row - 1);
            boolean selected = isNew ? editing == null : name.equals(editing);
            boolean hovered = mx >= listX && mx < listX + listW && my >= y && my < y + ROW;
            if (selected) g.fill(listX, y, listX + listW, y + ROW, SELECTED);
            else if (hovered) g.fill(listX, y, listX + listW, y + ROW, 0x66FFFFFF);

            if (isNew) {
                g.text(font, Component.translatable("breezelook.settings.new"), listX + 6, y + 7, 0xFFFFFFFF);
            } else {
                LookDirection d = Main.points.get(name);
                g.item(WarpIcons.stackFor(d != null ? d.icon() : null), listX + 3, y + 3);
                g.text(font, font.plainSubstrByWidth(name, listW - 28), listX + 24, y + 7, 0xFFFFFFFF);
            }
        }
    }

    private void drawGrid(GuiGraphicsExtractor g, int mx, int my) {
        int gridWidth = gridCols * CELL;
        g.fill(gridX - 1, gridY - 1, gridX + gridWidth + 1, gridY + gridRows * CELL + 1, 0x90000000);

        if (filtered.isEmpty()) {
            g.text(font, Component.translatable("breezelook.settings.no_icons"), gridX + 4, gridY + 6, 0xFFAAAAAA);
            return;
        }

        int hoveredIndex = gridIndexAt(mx, my);
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                int index = (gridScroll + row) * gridCols + col;
                if (index >= filtered.size()) return;
                Item item = filtered.get(index);
                int x = gridX + col * CELL;
                int y = gridY + row * CELL;
                boolean selected = WarpIcons.idOf(item).equals(selectedIcon);
                int background = selected ? SELECTED : (index == hoveredIndex ? 0xFF606060 : 0xFF2B2B2B);
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, background);
                ItemStack stack = new ItemStack(item);
                g.item(stack, x + 2, y + 2);
                if (index == hoveredIndex) g.setTooltipForNextFrame(font, stack, mx, my);
            }
        }

        int totalRows = totalGridRows();
        if (totalRows > gridRows) {
            int trackHeight = gridRows * CELL;
            int trackX = gridX + formW - 3;
            int thumbHeight = Math.max(8, trackHeight * gridRows / totalRows);
            int thumbY = gridY + (trackHeight - thumbHeight) * gridScroll / Math.max(1, totalRows - gridRows);
            g.fill(trackX, gridY, trackX + 3, gridY + trackHeight, 0x66FFFFFF);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }

    // ---------------------------------------------------------------- input

    /** Click inside the warp list: the first row creates a new point, the others load an existing one. */
    private void clickList(double mx, double my) {
        int relative = (int) ((my - listY) / ROW);
        if (relative < 0 || relative >= visibleRows()) return;
        int row = listScroll + relative;
        if (row == 0) newPoint();
        else if (row - 1 < names.size()) loadPoint(names.get(row - 1));
    }

    /** Click inside the icon grid: picks the icon, clicking the selected one again clears it. */
    private void clickGrid(double mx, double my) {
        int index = gridIndexAt(mx, my);
        if (index < 0) return;
        String id = WarpIcons.idOf(filtered.get(index));
        selectedIcon = id.equals(selectedIcon) ? null : id;
    }

    private boolean scrollList(double mx, double my, double amount) {
        int step = (int) -Math.signum(amount);
        if (step == 0) return false;
        int max = Math.max(0, names.size() + 1 - visibleRows());
        listScroll = Math.max(0, Math.min(max, listScroll + step));
        return true;
    }

    private boolean scrollGrid(double mx, double my, double amount) {
        int step = (int) -Math.signum(amount);
        if (step == 0) return false;
        gridScroll = Math.max(0, Math.min(maxGridScroll(), gridScroll + step));
        return true;
    }
}
