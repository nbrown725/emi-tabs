package com.nbrown725.emitabs.client.mixin;

import java.util.List;

import com.nbrown725.emitabs.client.CreativeTabs;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

// remap = false: we target EMI's own members (render/mouseClicked/lastWidth/...),
// whose names aren't in the Minecraft mappings. All @Inject/@Shadow below inherit it.
@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerMixin {

    // EMI's private layout-cache key. We write -1 to it to force one re-layout.
    @Shadow private static int lastWidth;

    // One-shot guard so we invalidate the layout exactly once, after tabs first build.
    @Unique private static boolean emitabs$invalidatedForTabs = false;

    @Inject(method = "render", at = @At("TAIL"))
    private static void emitabs$onRender(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CreativeTabs.ensureBuilt();
        List<CreativeTabs.Tab> tabs = CreativeTabs.getTabs();
        if (tabs.isEmpty()) {
            return;
        }

        // The first layout happened before our tabs existed, so EMI cached an
        // un-carved panel. Force exactly one re-layout now that tabs are present.
        if (!emitabs$invalidatedForTabs) {
            lastWidth = -1;
            emitabs$invalidatedForTabs = true;
        }

        // Anchor the tab row to the real item panel: same left edge as the grid,
        // sitting in the strip we carved just above it.
        EmiScreenManager.SidebarPanel panel = EmiScreenManager.getSearchPanel();
        if (panel == null || panel.space == null) {
            return;
        }
        // The carve pushed the grid (and EMI's page-select header, which is drawn
        // relative to the grid top) down by STRIP. That freed a strip ABOVE the
        // header, so place the tab row there: above both the header and the grid.
        int headerHeight = panel.header ? 18 : 0;
        CreativeTabs.rowX = panel.space.tx;
        CreativeTabs.rowY = panel.space.ty - CreativeTabs.STRIP - headerHeight + 1;

        // Work out the paging layout for this frame. The row has the same pixel
        // width as the grid (tw columns * ENTRY_SIZE). If every tab fits we show
        // them all; otherwise we reserve a slot at each end for the </> arrows.
        int total = tabs.size();
        CreativeTabs.panelW = panel.space.tw * 18; // 18 = EMI's ENTRY_SIZE (16 + 1px padding each side)
        if (total * CreativeTabs.SPACING <= CreativeTabs.panelW) {
            CreativeTabs.perPage = total;
            CreativeTabs.paged = false;
        } else {
            CreativeTabs.perPage = Math.max(1,
                (CreativeTabs.panelW - 2 * CreativeTabs.ARROW_W) / CreativeTabs.SPACING);
            CreativeTabs.paged = true;
        }
        CreativeTabs.totalPages = (total + CreativeTabs.perPage - 1) / CreativeTabs.perPage; // ceil
        CreativeTabs.page = Math.max(0, Math.min(CreativeTabs.page, CreativeTabs.totalPages - 1));
        // Arrows pin to the panel edges so they line up with EMI's own page arrows.
        CreativeTabs.rightArrowX = CreativeTabs.rowX + CreativeTabs.panelW - CreativeTabs.ARROW_W;
        if (CreativeTabs.paged) {
            // Center the tab block in the space between the two arrows.
            int between = CreativeTabs.panelW - 2 * CreativeTabs.ARROW_W;
            int blockW = CreativeTabs.perPage * CreativeTabs.SPACING;
            CreativeTabs.tabsStartX = CreativeTabs.rowX + CreativeTabs.ARROW_W + (between - blockW) / 2;
        } else {
            // No arrows: center the tabs across the whole panel width.
            int blockW = CreativeTabs.perPage * CreativeTabs.SPACING; // perPage == total here
            CreativeTabs.tabsStartX = CreativeTabs.rowX + (CreativeTabs.panelW - blockW) / 2;
        }

        int hovered = CreativeTabs.tabIndexAt(mouseX, mouseY);
        GuiGraphics g = context.raw();

        // Draw only the tabs on the current page, positioned by slot within the page.
        // Each tab sits in a native EMI 18px slot, with the icon inset 1px.
        int start = CreativeTabs.page * CreativeTabs.perPage;
        int end = Math.min(total, start + CreativeTabs.perPage);
        for (int i = start; i < end; i++) {
            int slot = i - start;
            int sx = CreativeTabs.tabsStartX + slot * CreativeTabs.SPACING;
            EmiTexture.SLOT.render(g, sx, CreativeTabs.rowY, delta);
            g.renderItem(tabs.get(i).icon, sx + 1, CreativeTabs.rowY + 1);
            if (i == CreativeTabs.selected) {
                // EMI's native slot highlight, persistent so the active tab always glows
                EmiRenderHelper.drawSlotHightlight(context, sx, CreativeTabs.rowY,
                        CreativeTabs.SPACING, CreativeTabs.SPACING, 0);
            } else if (i == hovered) {
                // fainter sheen for the tab under the cursor
                context.fill(sx, CreativeTabs.rowY, CreativeTabs.SPACING, CreativeTabs.SPACING, 0x40FFFFFF);
            }
        }

        // Paging arrows at the row ends, using EMI's own button sprites so they
        // match the page arrows below. v offset mirrors EMI's SizedButtonWidget:
        // +SIZE on hover, +SIZE*2 when disabled (can't page that way).
        if (CreativeTabs.paged) {
            int leftX = CreativeTabs.rowX;
            int rightX = CreativeTabs.rightArrowX;
            int ay = CreativeTabs.rowY + (CreativeTabs.SPACING - CreativeTabs.SIZE) / 2; // center 16 in 18
            boolean canLeft = CreativeTabs.page > 0;
            boolean canRight = CreativeTabs.page < CreativeTabs.totalPages - 1;
            int hover = CreativeTabs.arrowAt(mouseX, mouseY);
            int leftV = !canLeft ? CreativeTabs.SIZE * 2 : (hover == -1 ? CreativeTabs.SIZE : 0);
            int rightV = !canRight ? CreativeTabs.SIZE * 2 : (hover == 1 ? CreativeTabs.SIZE : 0);
            context.drawTexture(EmiRenderHelper.BUTTONS, leftX, ay, 224, leftV, CreativeTabs.SIZE, CreativeTabs.SIZE);
            context.drawTexture(EmiRenderHelper.BUTTONS, rightX, ay, 240, rightV, CreativeTabs.SIZE, CreativeTabs.SIZE);
        }

        // Tooltip for the hovered tab. Drawn last so it sits on top.
        if (hovered >= 0) {
            g.renderTooltip(Minecraft.getInstance().font, tabs.get(hovered).name, mouseX, mouseY);
        }
    }

    // Carve one row of vertical space out of the top of the item index panel,
    // by adjusting the arguments passed to `new ScreenSpace(tx, ty, tw, th, ...)`.
    // arg 1 = ty (grid top), arg 3 = th (row count), arg 7 = search (is this the index panel?).
    @ModifyArgs(method = "createScreenSpace",
            at = @At(value = "INVOKE", ordinal = 0, remap = false,
                    target = "dev/emi/emi/screen/EmiScreenManager$ScreenSpace.<init>(IIIIZLjava/util/List;Ljava/util/function/Supplier;Z)V"))
    private static void emitabs$carve(Args args) {
        boolean search = args.get(7);
        if (!search || CreativeTabs.getTabs().isEmpty()) {
            return; // only the index panel, and only once we actually have tabs to show
        }
        int ty = args.get(1);
        int th = args.get(3);
        if (th <= 1) {
            return; // not enough rows to give one up
        }
        args.set(1, ty + CreativeTabs.STRIP); // push the grid top down
        args.set(3, th - 1);                  // and drop one row to make room
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private static void emitabs$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Paging arrow? Flip the page but DON'T repopulate: paging only changes
        // which tab buttons are visible, not `selected`, so the item list is
        // unchanged and rebuilding its batched render would be wasted work.
        int arrow = CreativeTabs.arrowAt(mouseX, mouseY);
        if (arrow != 0) {
            int newPage = CreativeTabs.page + arrow;
            if (newPage >= 0 && newPage < CreativeTabs.totalPages) {
                CreativeTabs.page = newPage;
            }
            cir.setReturnValue(true); // consume so EMI doesn't grab an item underneath
            return;
        }

        int i = CreativeTabs.tabIndexAt(mouseX, mouseY);
        if (i >= 0) {
            // click the active tab again to deselect (-1 = show everything)
            CreativeTabs.selected = (CreativeTabs.selected == i) ? -1 : i;

            // The displayed list just changed, so refresh EMI's cached render:
            // jump back to the first page and mark the batched items dirty
            // (the same repopulate() call EMI makes when its search changes).
            EmiScreenManager.SidebarPanel searchPanel = EmiScreenManager.getSearchPanel();
            if (searchPanel != null) {
                searchPanel.page = 0;
                searchPanel.space.batcher.repopulate();
            }

            cir.setReturnValue(true); // consume the click so EMI doesn't also grab an item
        }
    }
}
