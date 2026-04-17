package com.github.epsilon;

import com.github.epsilon.assets.i18n.LanguageReloadListener;
import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.graphics.LuminRenderPipelines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EpsilonFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Epsilon.VERSION = FabricLoader.getInstance().getModContainer(Epsilon.MODID).get()
                .getMetadata().getVersion().getFriendlyString();
        Epsilon.platform = new FabricPlatformCompat();

        Epsilon.init();
        CommonListeners.register();

        // Register render pipelines
        LuminRenderPipelines.registerAll(pipeline -> {
            // Pipelines are auto-registered when referenced in vanilla 26.1.2+
        });

        // Register resource reload listener (equivalent to NeoForge's AddClientReloadListenersEvent)
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new FabricReloadListenerWrapper(
                        ResourceLocationUtils.getIdentifier("objects/reload_listener"),
                        new LanguageReloadListener()
                )
        );

        Epsilon.LOGGER.info("Epsilon Fabric loaded successfully!");
    }

    private record FabricReloadListenerWrapper(
            Identifier id,
            PreparableReloadListener delegate
    ) implements IdentifiableResourceReloadListener {

        @Override
        public Identifier getFabricId() {
            return id;
        }

        @Override
        public CompletableFuture<Void> reload(
                PreparableReloadListener.SharedState sharedState,
                Executor backgroundExecutor,
                PreparableReloadListener.PreparationBarrier barrier,
                Executor gameExecutor
        ) {
            return delegate.reload(sharedState, backgroundExecutor, barrier, gameExecutor);
        }
    }

}
