package sk.breezelook.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A plain rectangular widget that hands painting, clicks and scrolling to the screen that owns it.
 * Going through a real widget means the vanilla click routing (child under the cursor) is used,
 * the same way buttons and text boxes are.
 */
final class AreaWidget extends AbstractWidget {
    interface Painter {
        void paint(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
    }

    interface ClickHandler {
        void click(double mouseX, double mouseY);
    }

    interface ScrollHandler {
        boolean scroll(double mouseX, double mouseY, double amount);
    }

    private final Painter painter;
    private final ClickHandler clickHandler;
    private final ScrollHandler scrollHandler;

    AreaWidget(int x, int y, int width, int height, Painter painter, ClickHandler clickHandler, ScrollHandler scrollHandler) {
        super(x, y, width, height, Component.empty());
        this.painter = painter;
        this.clickHandler = clickHandler;
        this.scrollHandler = scrollHandler;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        painter.paint(graphics, mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        clickHandler.click(event.x(), event.y());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrollHandler.scroll(mouseX, mouseY, scrollY);
    }
}
