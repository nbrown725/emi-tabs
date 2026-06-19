package com.nbrown725.emitabs.client;

import net.fabricmc.api.ClientModInitializer;
import com.nbrown725.emitabs.EmiTabs;

public class EmiTabsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        EmiTabs.LOGGER.info("EMI Tabs client initializing");
    }
}
