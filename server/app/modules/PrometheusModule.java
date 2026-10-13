package modules;

import com.typesafe.config.Config;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

/**
 * Prepares the Prometheus {@link CollectorRegistry} for this application instance and exposes it
 * for injection.
 */
public final class PrometheusModule extends Module {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusModule.class);

  static final String REGISTER_DEFAULT_HOTSPOT_COLLECTORS_KEY =
      "prometheus.register_default_hotspot_collectors";

  @Override
  public List<Binding<?>> bindings(Environment environment, Config config) {
    CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    registry.clear();

    if (config.hasPath(REGISTER_DEFAULT_HOTSPOT_COLLECTORS_KEY)
        && config.getBoolean(REGISTER_DEFAULT_HOTSPOT_COLLECTORS_KEY)) {
      DefaultExports.register(registry);
      logger.info("Prometheus default hotspot collectors registered");
    } else {
      logger.info("Prometheus default hotspot collectors are disabled");
    }

    return List.of(bindClass(CollectorRegistry.class).toInstance(registry));
  }
}
