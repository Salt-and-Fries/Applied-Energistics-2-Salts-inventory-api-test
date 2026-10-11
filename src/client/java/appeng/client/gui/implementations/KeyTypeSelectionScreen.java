package appeng.client.gui.implementations;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.storage.ISubMenuHost;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.AEBaseMenu;
import appeng.menu.interfaces.KeyTypeSelectionMenu;
import appeng.util.Icon;

public class KeyTypeSelectionScreen<C extends AEBaseMenu & KeyTypeSelectionMenu, P extends AEBaseScreen<C>>
        extends AESubScreen<C, P> {
    private final KeyTypeCheckboxes keyTypesWidget = new KeyTypeCheckboxes();

    public KeyTypeSelectionScreen(P parent, ISubMenuHost subMenuHost, Component dialogTitle) {
        super(parent, "/screens/key_type_selection.json");

        addBackButton(subMenuHost);

        widgets.add("keytypes", keyTypesWidget);

        setTextContent("dialog_title", dialogTitle);
    }

    private void addBackButton(ISubMenuHost subMenuHost) {
        var icon = subMenuHost.getMainMenuIcon();
        var label = icon.getHoverName();
        TabButton button = new TabButton(Icon.BACK, label, btn -> returnToParent());
        widgets.add("back", button);
    }

    private void setHeight(int height) {
        this.style.getGeneratedBackground().setHeight(height);
        this.imageHeight = height;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        int selectedEntryCount = 0;
        AECheckbox selectedEntry = null;

        for (var entry : keyTypesWidget.checkboxes.entrySet()) {
            boolean selected = getMenu().getClientKeyTypeSelection().keyTypes().get(entry.getKey());
            entry.getValue().setSelected(selected);
            entry.getValue().active = true;

            if (selected) {
                selectedEntryCount++;
                selectedEntry = entry.getValue();
            }
        }

        // Prevent disabling all the key types
        if (selectedEntryCount == 1) {
            selectedEntry.active = false;
        }
    }

    private class KeyTypeCheckboxes implements ICompositeWidget {
        private static final int PADDING = 6;
        private static final int KEY_TYPE_SPACING = AECheckbox.SIZE + PADDING;

        private Rect2i bounds = new Rect2i(0, 0, 0, 0);
        private final Map<AEKeyType, AECheckbox> checkboxes = new LinkedHashMap<>();

        @Override
        public void setPosition(Point position) {
            bounds = new Rect2i(position.getX(), position.getY(), bounds.getWidth(), bounds.getHeight());
        }

        @Override
        public void setSize(int width, int height) {
            bounds = new Rect2i(bounds.getX(), bounds.getY(), width, height);
        }

        @Override
        public Rect2i getBounds() {
            return bounds;
        }

        @Override
        public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
            int xPos = this.bounds.getX() + bounds.getX();
            int yPos = this.bounds.getY() + bounds.getY();

            if (screen.isDesktopHosted()) {
                checkboxes.keySet().retainAll(getMenu().getClientKeyTypeSelection().keyTypes().keySet());
            } else {
                checkboxes.clear();
            }

            // The model map has no stable iteration order across JVMs. Use the same public type-id
            // order for ordinary and hosted windows so each choice keeps the same row.
            var keyTypes = getMenu().getClientKeyTypeSelection().keyTypes().keySet().stream()
                    .sorted(Comparator.comparing(keyType -> keyType.getId().toString())).toList();
            for (var keyType : keyTypes) {
                var text = keyType.getDescription();

                // Dynamic width based on text size
                int textboxWidth = (screen.isDesktopHosted() ? 28 : 24)
                        + Minecraft.getInstance().font.width(text);
                if (screen.isDesktopHosted()) {
                    int available = Math.max(28, screen.desktopContentBounds().getWidth() - this.bounds.getX() - 8);
                    textboxWidth = Math.min(textboxWidth, available);
                }

                var checkbox = checkboxes.get(keyType);
                if (checkbox == null) {
                    checkbox = new AECheckbox(xPos, yPos, textboxWidth, AECheckbox.SIZE, screen.getStyle(), text);
                    var selectedCheckbox = checkbox;
                    checkbox.setChangeListener(() -> getMenu().selectKeyType(keyType, selectedCheckbox.isSelected()));
                    checkboxes.put(keyType, checkbox);
                }
                checkbox.setX(xPos);
                checkbox.setY(yPos);
                checkbox.setWidth(textboxWidth);
                if (screen.isDesktopHosted()) {
                    checkbox.setHeight(Math.max(AECheckbox.SIZE,
                            Minecraft.getInstance().font.split(text, textboxWidth - 26).size() * 9 + 2));
                }
                addWidget.accept(checkbox);

                yPos += screen.isDesktopHosted() ? checkbox.getHeight() + PADDING
                        : KEY_TYPE_SPACING;
            }

            int height = screen.isDesktopHosted() ? yPos - bounds.getY() + PADDING
                    : this.bounds.getY() + AEKeyTypes.getAll().size() * KEY_TYPE_SPACING + PADDING;
            KeyTypeSelectionScreen.this.setHeight(height);
        }
    }
}
