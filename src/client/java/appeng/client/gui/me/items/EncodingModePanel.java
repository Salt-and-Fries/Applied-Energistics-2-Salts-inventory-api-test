package appeng.client.gui.me.items;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.util.Icon;

public abstract class EncodingModePanel implements ICompositeWidget {
    protected final PatternEncodingTermScreen<?> screen;
    protected final PatternEncodingTermMenu menu;
    protected final WidgetContainer widgets;
    protected boolean visible = false;
    protected int x;
    protected int y;

    protected final void drawModeBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            Rect2i bounds, appeng.client.gui.style.Blitter ordinary) {
        if ((screen.isDesktopHosted() && !screen.usesAeDesktopLayout())) {
            graphics.fill(bounds.getX() + x, bounds.getY() + y,
                    bounds.getX() + x + 124, bounds.getY() + y + 66, 0xFFB8B8B8);
            if (!(this instanceof StonecuttingEncodingPanel)) {
                appeng.client.gui.DesktopChrome.arrow(graphics, bounds.getX() + x + 69, bounds.getY() + y + 26);
            }
        } else {
            ordinary.dest(bounds.getX() + 8, bounds.getY() + bounds.getHeight() - 165).blit(graphics);
        }
    }

    public EncodingModePanel(PatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
        this.screen = screen;
        this.menu = screen.getMenu();
        this.widgets = widgets;
    }

    // TODO (Rid): Replaced the ItemStack and an Icon
//    abstract ItemStack getTabIconItem();

    abstract Icon getIcon();

    abstract Component getTabTooltip();

    @Override
    public void setPosition(Point position) {
        x = position.getX();
        y = position.getY();
    }

    @Override
    public void setSize(int width, int height) {
    }

    @Override
    public Rect2i getBounds() {
        return new Rect2i(x, y, 124, 66);
    }

    @Override
    public final boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
