package com.nbrown725.emitabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared, loader-agnostic constants and setup. The per-loader entrypoints
 * (EmiTabsFabric / EmiTabsNeoForge) just call into this.
 */
public final class EmiTabs {
    public static final String MOD_ID = "emitabs";

    // Logged under the mod id so it's clear which mod wrote each line. slf4j is
    // available on both Fabric and NeoForge, so this stays in common.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private EmiTabs() {}

    public static void init() {
        LOGGER.info("EMI Tabs initializing");
    }
}
