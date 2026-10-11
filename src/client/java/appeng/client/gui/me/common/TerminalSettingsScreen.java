package appeng.client.gui.me.common;

import net.minecraft.network.chat.Component;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.integration.abstraction.ItemListMod;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.MEStorageMenu;
import appeng.util.Icon;

public class TerminalSettingsScreen<C extends MEStorageMenu> extends AESubScreen<C, MEStorageScreen<C>> {

    private final AECheckbox pinAutoCraftedItemsCheckbox;
    private final AECheckbox notifyForFinishedCraftingJobsCheckbox;
    private final AECheckbox clearGridOnCloseCheckbox;

    private final AECheckbox useInternalSearchRadio;
    private final AECheckbox useExternalSearchRadio;

    private final AECheckbox rememberCheckbox;
    private final AECheckbox autoFocusCheckbox;
    private final AECheckbox syncWithExternalCheckbox;
    private final AECheckbox clearExternalCheckbox;

    public TerminalSettingsScreen(MEStorageScreen<C> parent) {
        super(parent, "/screens/terminals/terminal_settings.json");

        addBackButton();

        Component externalSearchMod;
        boolean hasExternalSearch;
        if (ItemListMod.isEnabled()) {
            externalSearchMod = Component.literal(ItemListMod.getShortName());
            hasExternalSearch = true;
        } else {
            // User doesn't have either, so disable the buttons but show what *would* be possible
            externalSearchMod = Component.literal("REI/EMI");
            hasExternalSearch = false;
        }

        pinAutoCraftedItemsCheckbox = widgets.addCheckbox("pinAutoCraftedItemsCheckbox",
                GuiText.TerminalSettingsPinAutoCraftedItems.text(), this::save);
        notifyForFinishedCraftingJobsCheckbox = widgets.addCheckbox("notifyForFinishedCraftingJobsCheckbox",
                GuiText.TerminalSettingsNotifyForFinishedJobs.text(), this::save);
        clearGridOnCloseCheckbox = widgets.addCheckbox("clearGridOnCloseCheckbox",
                GuiText.TerminalSettingsClearGridOnClose.text(), this::save);
        if (style.isDesktopRestyled()) {
            pinAutoCraftedItemsCheckbox.setDesktopLabel(Component.translatable("gui.ae2.DesktopSettings.pinCrafted"));
            notifyForFinishedCraftingJobsCheckbox
                    .setDesktopLabel(Component.translatable("gui.ae2.DesktopSettings.notifyCrafted"));
            clearGridOnCloseCheckbox.setDesktopLabel(Component.translatable("gui.ae2.DesktopSettings.clearGrid"));
        }

        useInternalSearchRadio = widgets.addCheckbox("useInternalSearchRadio",
                GuiText.SearchSettingsUseInternalSearch.text(), this::switchToAeSearch);
        useInternalSearchRadio.setRadio(true);
        useExternalSearchRadio = widgets.addCheckbox("useExternalSearchRadio",
                GuiText.SearchSettingsUseExternalSearch.text(externalSearchMod), this::switchToExternalSearch);
        useExternalSearchRadio.setRadio(true);
        useExternalSearchRadio.active = hasExternalSearch;

        rememberCheckbox = widgets.addCheckbox("rememberCheckbox", GuiText.SearchSettingsRememberSearch.text(),
                this::save);
        autoFocusCheckbox = widgets.addCheckbox("autoFocusCheckbox", GuiText.SearchSettingsAutoFocus.text(),
                this::save);

        syncWithExternalCheckbox = widgets.addCheckbox("syncWithExternalCheckbox",
                GuiText.SearchSettingsSyncWithExternal.text(externalSearchMod), this::save);
        clearExternalCheckbox = widgets.addCheckbox("clearExternalCheckbox",
                GuiText.SearchSettingsClearExternal.text(externalSearchMod), this::save);

        updateState();
    }

    @Override
    protected void init() {
        if (isDesktopHosted()) {
            int y = 24;
            y = placeCheckbox("pinAutoCraftedItemsCheckbox", pinAutoCraftedItemsCheckbox, 8, y, imageWidth - 16);
            y = placeCheckbox("notifyForFinishedCraftingJobsCheckbox", notifyForFinishedCraftingJobsCheckbox, 8, y,
                    imageWidth - 16);
            y = placeCheckbox("clearGridOnCloseCheckbox", clearGridOnCloseCheckbox, 8, y, imageWidth - 16);
            style.getText().get("search_settings_title").getPosition().setTop(y + 8);
            y += 22;
            int radioWidth = (imageWidth - 24) / 2;
            int radioEnd = placeCheckbox("useInternalSearchRadio", useInternalSearchRadio, 8, y, radioWidth);
            y = Math.max(radioEnd, placeCheckbox("useExternalSearchRadio", useExternalSearchRadio,
                    imageWidth / 2 + 4, y, radioWidth)) + 2;
            y = placeCheckbox("rememberCheckbox", rememberCheckbox, 8, y, imageWidth - 16);
            y = placeCheckbox("autoFocusCheckbox", autoFocusCheckbox, 8, y, imageWidth - 16);
            y = placeCheckbox("syncWithExternalCheckbox", syncWithExternalCheckbox, 8, y, imageWidth - 16);
            if (clearExternalCheckbox.visible) {
                y = placeCheckbox("clearExternalCheckbox", clearExternalCheckbox, 8, y, imageWidth - 16);
            }
            style.getGeneratedBackground().setHeight(y + 4);
        }
        super.init();

        // The screen JSON includes the toolbox, but we don't actually have a need for it here
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private int placeCheckbox(String id, AECheckbox checkbox, int x, int y, int width) {
        if (checkbox == null || !checkbox.visible) {
            return y;
        }
        int height = Math.max(AECheckbox.SIZE,
                font.split(checkbox.displayLabel(), width - (checkbox.isRadio() ? 16 : 26))
                        .size() * font.lineHeight + 2);
        var position = style.getWidget(id);
        position.setLeft(x);
        position.setTop(y);
        checkbox.setWidth(width);
        checkbox.setHeight(height);
        return y + height + 4;
    }

    private void switchToAeSearch() {
        useInternalSearchRadio.setSelected(true);
        useExternalSearchRadio.setSelected(false);
        save();
    }

    private void switchToExternalSearch() {
        useInternalSearchRadio.setSelected(false);
        useExternalSearchRadio.setSelected(true);
        save();
    }

    private void addBackButton() {
        var label = menu.getHost().getMainMenuIcon().getHoverName();
        TabButton button = new TabButton(Icon.BACK, label, btn -> returnToParent());
        widgets.add("back", button);
    }

    private void updateState() {
        pinAutoCraftedItemsCheckbox.setSelected(config.isPinAutoCraftedItems());
        notifyForFinishedCraftingJobsCheckbox.setSelected(config.isNotifyForFinishedCraftingJobs());
        clearGridOnCloseCheckbox.setSelected(config.isClearGridOnClose());

        useInternalSearchRadio.setSelected(!config.isUseExternalSearch());
        useExternalSearchRadio.setSelected(config.isUseExternalSearch());
        rememberCheckbox.setSelected(config.isRememberLastSearch());
        autoFocusCheckbox.setSelected(config.isAutoFocusSearch());
        syncWithExternalCheckbox.setSelected(config.isSyncWithExternalSearch());
        clearExternalCheckbox.setSelected(config.isClearExternalSearchOnOpen());

        rememberCheckbox.visible = useInternalSearchRadio.isSelected();
        autoFocusCheckbox.visible = useInternalSearchRadio.isSelected();
        syncWithExternalCheckbox.visible = useInternalSearchRadio.isSelected();

        clearExternalCheckbox.visible = useExternalSearchRadio.isSelected();
        if (isDesktopHosted()) {
            refreshDesktopContentLayout();
        }
    }

    private void save() {
        config.setUseExternalSearch(useExternalSearchRadio.isSelected());
        config.setRememberLastSearch(rememberCheckbox.isSelected());
        config.setAutoFocusSearch(autoFocusCheckbox.isSelected());
        config.setSyncWithExternalSearch(syncWithExternalCheckbox.isSelected());
        config.setClearExternalSearchOnOpen(clearExternalCheckbox.isSelected());
        config.setPinAutoCraftedItems(pinAutoCraftedItemsCheckbox.isSelected());
        config.setNotifyForFinishedCraftingJobs(notifyForFinishedCraftingJobsCheckbox.isSelected());
        config.setClearGridOnClose(clearGridOnCloseCheckbox.isSelected());

        updateState();
    }
}
