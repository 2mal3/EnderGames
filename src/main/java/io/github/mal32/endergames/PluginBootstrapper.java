package io.github.mal32.endergames;

import io.papermc.paper.datapack.DatapackRegistrar;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PluginBootstrapper implements PluginBootstrap {
  @Override
  public void bootstrap(BootstrapContext context) {
    final LifecycleEventManager<@org.jetbrains.annotations.NotNull BootstrapContext> manager =
        context.getLifecycleManager();
    manager.registerEventHandler(
        LifecycleEvents.DATAPACK_DISCOVERY,
        event -> {
          DatapackRegistrar registrar = event.registrar();
          try {
            final URI uri =
                Objects.requireNonNull(PluginBootstrapper.class.getResource("/EnderGamesDatapack"))
                    .toURI();
            registrar.discoverPack(uri, "endergames");
          } catch (final URISyntaxException | IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public @NotNull JavaPlugin createPlugin(PluginProviderContext context) {
    return new EnderGames();
  }
}
