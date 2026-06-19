package com.nbrown725.emitabs.fabric;

import com.nbrown725.emitabs.EmiTabs;

import net.fabricmc.api.ModInitializer;

public class EmiTabsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EmiTabs.init();
    }
}
