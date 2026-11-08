package sk.breezelook.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import sk.breezelook.Main;
import sk.breezelook.Main.LookDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Radial menu for warp points. Opened by holding the wheel key: move the mouse over a segment
 * and release the key (or click) to turn the camera to that point. The gear on the right opens
 * the point editor.
 */
public class WarpWheelScreen extends Screen {
    private static final int BASE = 0x99101418;
    private static final int HOVER = 0xCC3A7BD5;
    private static final int GEAR_BOX = 30;

    private final KeyMapping openKey;
    private final List<String> names = new ArrayList<>();

    private int cx, cy, outer, inner, gearX, gearY;
    private int[] cells = new int[0];
    private int mouseX, mouseY;
    private boolean closing;

    public WarpWheelScreen(KeyMapping openKey) {
        super(Component.translatable("breezelook.wheel.title"));
        this.openKey = openKey;
    }

    @Override
    protected void init() {
        names.clear();
        names.addAll(Main.points.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);

        cx = width / 2;
        cy = height / 2;
        mouseX = cx;
        mouseY = cy;
        outer = Math.max(40, Math.min(96, height / 2 - 32));
        inner = (int) (outer * 0.36);
        gearX = Math.min(cx + outer + 14, width - GEAR_BOX - 4);
        gearY = cy - GEAR_BOX / 2;
        buildCells();
    }

    /** Precomputes which segment each pixel of the ring belongs to (-1 = not part of the ring). */
    private void buildCells() {
        int size = outer * 2 + 1;
        cells = new int[size * size];
        int n = names.size();
        double seg = n > 0 ? 2 * Math.PI / n : 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - outer;
                double dy = y - outer;
                double r = Math.sqrt(dx * dx + dy * dy);
                int value;
                if (r < inner || r > outer) {
                    value = -1;
                } else if (n == 0) {
                    value = 0;
                } else {
                    double shifted = angleFromTop(dx, dy) + seg / 2;
                    shifted %= 2 * Math.PI;
                    int index = Math.min(n - 1, (int) (shifted / seg));
                    double within = shifted - index * seg;
                    double edge = Math.min(within, seg - within);
                    value = (n > 1 && edge * r < 1.2) ? -1 : index;
                }
                cells[y * size + x] = value;
            }
        }
    }

    /** Angle in radians, 0 at the top and growing clockwise. */
    private static double angleFromTop(double dx, double dy) {
        double angle = Math.atan2(dx, -dy);
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }

    private int hoveredIndex(int mx, int my) {
        int n = names.size();
        if (n == 0) return -1;
        double dx = mx - cx;
        double dy = my - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < inner || r > outer) return -1;
        double seg = 2 * Math.PI / n;
        double shifted = (angleFromTop(dx, dy) + seg / 2) % (2 * Math.PI);
        return Math.min(n - 1, (int) (shifted / seg));
    }

    private boolean overGear(int mx, int my) {
        return mx >= gearX && mx < gearX + GEAR_BOX && my >= gearY && my < gearY + GEAR_BOX;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        // keep the world fully visible behind the wheel
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        mouseX = mx;
        mouseY = my;
        int hovered = hoveredIndex(mx, my);
        boolean gearHovered = overGear(mx, my);

        drawRing(g, hovered);
        drawEntries(g, hovered);
        drawGear(g, gearHovered);

        g.centeredText(font, title, cx, cy - outer - 14, 0xFFFFFFFF);
        int textY = cy + outer + 6;
        if (names.isEmpty()) {
            g.centeredText(font, Component.translatable("breezelook.wheel.empty"), cx, textY, 0xFFFFFFFF);
            g.centeredText(font, Component.translatable("breezelook.wheel.empty_hint"), cx, textY + 11, 0xFFAAAAAA);
        } else if (hovered >= 0) {
            String name = names.get(hovered);
            LookDirection d = Main.points.get(name);
            g.centeredText(font, name, cx, textY, 0xFFFFFFFF);
            if (d != null) {
                String angles = String.format(Locale.ROOT, "H %.1f   V %.1f", d.horizontal(), d.vertical());
                g.centeredText(font, angles, cx, textY + 11, 0xFFAAAAAA);
            }
        }

        if (gearHovered) {
            g.setTooltipForNextFrame(font, Component.translatable("breezelook.wheel.settings"), mx, my);
        }
    }

    private void drawRing(GuiGraphicsExtractor g, int hovered) {
        int size = outer * 2 + 1;
        int originX = cx - outer;
        int originY = cy - outer;
        for (int y = 0; y < size; y++) {
            int x = 0;
            while (x < size) {
                int value = cells[y * size + x];
                if (value < 0) {
                    x++;
                    continue;
                }
                int end = x + 1;
                while (end < size && cells[y * size + end] == value) end++;
                g.fill(originX + x, originY + y, originX + end, originY + y + 1, value == hovered ? HOVER : BASE);
                x = end;
            }
        }
    }

    private void drawEntries(GuiGraphicsExtractor g, int hovered) {
        int n = names.size();
        if (n == 0) return;
        double seg = 2 * Math.PI / n;
        double mid = (inner + outer) / 2.0;
        double chord = n > 1 ? 2 * mid * Math.sin(Math.PI / n) : 2 * mid;
        int labelWidth = (int) chord - 6;
        boolean showLabels = labelWidth >= 26;

        for (int i = 0; i < n; i++) {
            double angle = i * seg;
            int ix = cx + (int) Math.round(mid * Math.sin(angle));
            int iy = cy - (int) Math.round(mid * Math.cos(angle));
            LookDirection d = Main.points.get(names.get(i));
            g.item(WarpIcons.stackFor(d != null ? d.icon() : null), ix - 8, showLabels ? iy - 12 : iy - 8);
            if (showLabels) {
                g.centeredText(font, trim(names.get(i), labelWidth), ix, iy + 6, i == hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            }
        }
    }

    private String trim(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /** A small pixel-art gear, drawn procedurally so no texture is needed. */
    private void drawGear(GuiGraphicsExtractor g, boolean hovered) {
        g.fill(gearX, gearY, gearX + GEAR_BOX, gearY + GEAR_BOX, hovered ? HOVER : BASE);
        int color = hovered ? 0xFFFFFFFF : 0xFFE0E0E0;
        int grid = 13;
        int scale = 2;
        int startX = gearX + (GEAR_BOX - grid * scale) / 2;
        int startY = gearY + (GEAR_BOX - grid * scale) / 2;
        double quarter = Math.PI / 4;
        for (int py = 0; py < grid; py++) {
            for (int px = 0; px < grid; px++) {
                double dx = px - 6;
                double dy = py - 6;
                double dist = Math.sqrt(dx * dx + dy * dy);
                boolean filled = dist >= 2.2 && dist <= 4.4;
                if (!filled && dist > 4.0 && dist <= 6.3) {
                    double a = ((Math.atan2(dy, dx) % quarter) + quarter) % quarter;
                    filled = Math.min(a, quarter - a) < 0.30;
                }
                if (filled) {
                    int x = startX + px * scale;
                    int y = startY + py * scale;
                    g.fill(x, y, x + scale, y + scale, color);
                }
            }
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = (int) x;
        mouseY = (int) y;
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            mouseX = (int) event.x();
            mouseY = (int) event.y();
            finish();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (openKey.matchesMouse(event)) {
            finish();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (openKey.matches(event)) {
            finish();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (closing) return;
        // Safety net in case the key release event was missed (e.g. a very quick tap).
        InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(openKey);
        if (bound.getType() == InputConstants.Type.KEYBOARD && !InputConstants.isKeyDown(bound.getValue())) {
            finish();
        }
    }

    /** Applies whatever is under the cursor, then leaves the wheel. */
    private void finish() {
        if (closing) return;
        closing = true;
        int hovered = hoveredIndex(mouseX, mouseY);
        if (hovered >= 0) {
            String name = names.get(hovered);
            onClose();
            Main.warpTo(name);
        } else if (overGear(mouseX, mouseY)) {
            minecraft.setScreenAndShow(new WarpSettingsScreen());
        } else {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
