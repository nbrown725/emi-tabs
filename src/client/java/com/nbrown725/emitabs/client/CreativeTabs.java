package com.nbrown725.emitabs.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nbrown725.emitabs.EmiTabs;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;


public class CreativeTabs {
    public static class Tab {
        public final Component name;
        public final ItemStack icon;
        public final Set<Item> items;

        public Tab(Component name, ItemStack icon, Set<Item> items) {
            this.name = name;
            this.icon = icon;
            this.items = items;
        }
    }

    private static final List<Tab> TABS = new ArrayList<>();
    private static boolean built = false;

    public static int selected = -1;

    public static List<Tab> getTabs() {
        return TABS;
    }

    // Tab-row geometry. rowX/rowY and the paging fields below are recomputed each
    // frame from EMI's panel position (see EmiScreenManagerMixin#render); the
    // constants are fixed.
    public static int rowX = 4;
    public static int rowY = 20;
    public static final int SPACING = 18; // one 18px slot pitch, so tabs tile edge-to-edge like a hotbar
    public static final int SIZE = 16;    // icon / arrow-button size in px
    public static final int STRIP = 18;   // vertical space carved from the panel (one grid row)
    public static final int ARROW_W = 16; // px reserved at each end for a paging arrow (EMI's button is 16 wide)

    // Paging state. The tab row only has the panel's width to work with, so when
    // there are more tabs than fit we show one page at a time with </> arrows.
    public static int page = 0;          // current page
    public static int perPage = 0;       // tabs shown per page (computed each frame)
    public static int totalPages = 1;    // number of pages (computed each frame)
    public static boolean paged = false; // true when there are too many tabs to fit at once
    public static int panelW = 0;        // grid width in px (tw * ENTRY_SIZE)
    public static int tabsStartX = 0;    // x of the first *visible* tab
    public static int rightArrowX = 0;   // x of the right arrow, pinned flush against the tab block

    public static int tabIndexAt(double mouseX, double mouseY) {
        // The whole 18px slot is clickable, not just the 16px icon.
        if (mouseY < rowY || mouseY >= rowY + SPACING) {
            return -1;
        }

        // Only the tabs on the current page are on screen. Map the cursor to a
        // visible slot, then translate that slot back to an absolute tab index.
        int start = page * perPage;
        int visible = Math.min(TABS.size(), start + perPage) - start;
        for (int slot = 0; slot < visible; slot++) {
            int sx = tabsStartX + slot * SPACING;
            if (mouseX >= sx && mouseX < sx + SPACING) {
                return start + slot;
            }
        }

        return -1;
    }

    // Which paging arrow (if any) is under the cursor: -1 = left, +1 = right, 0 = none.
    public static int arrowAt(double mouseX, double mouseY) {
        if (!paged || mouseY < rowY || mouseY >= rowY + SPACING) {
            return 0;
        }
        if (mouseX >= rowX && mouseX < rowX + ARROW_W) {
            return -1;
        }
        if (mouseX >= rightArrowX && mouseX < rightArrowX + ARROW_W) {
            return 1;
        }
        return 0;
    }

    public static boolean matchesSelected(EmiIngredient ingredient) {
        if (selected < 0 || selected >= TABS.size()) {
            return true;
        }

        Set<Item> items = TABS.get(selected).items;
        for (EmiStack stack : ingredient.getEmiStacks()) {
            ItemStack is = stack.getItemStack();
            if (!is.isEmpty() && items.contains(is.getItem())) {
                return true;
            }
        }

        return false;
    }

    public static void ensureBuilt() {
        if (built) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
            mc.player.connection.enabledFeatures(), false, mc.level.registryAccess());

        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }

            try {
                tab.buildContents(params);
                Set<Item> items = new HashSet<>();
                for (ItemStack stack : tab.getDisplayItems()) {
                    items.add(stack.getItem());
                }
                TABS.add(new Tab(tab.getDisplayName(), tab.getIconItem(), items));
            } catch (Exception e) {
                EmiTabs.LOGGER.error("Failed to read a creative tab", e);
            }
        }

        built = true;
        EmiTabs.LOGGER.info("Built {} creative tabs", TABS.size());
    }
}
