package com.nbrown725.emitabs.client.mixin;

import java.util.List;
import java.util.stream.Collectors;

import com.nbrown725.emitabs.client.CreativeTabs;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EmiScreenManager.ScreenSpace.class)
public class ScreenSpaceMixin {

    @Shadow @Final public boolean search;

    @Inject(method = "getStacks", at = @At("RETURN"), cancellable = true)
    private void emitabs$filter(CallbackInfoReturnable<List<? extends EmiIngredient>> cir) {
        if (!search || CreativeTabs.selected < 0) {
            return;
        }

        List<? extends EmiIngredient> filtered = cir.getReturnValue().stream()
            .filter(CreativeTabs::matchesSelected)
            .collect(Collectors.toList());
        cir.setReturnValue(filtered);
    }
}
