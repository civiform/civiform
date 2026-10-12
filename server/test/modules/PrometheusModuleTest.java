package modules;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import play.Environment;
import play.inject.Binding;

public class PrometheusModuleTest {

  @Test
  public void bindings_clearsDefaultRegistry() {
    Counter.build().name("stale_counter").help("stale").register();

    new PrometheusModule().bindings(Environment.simple(), ConfigFactory.empty());

    assertThat(familyNames()).doesNotContain("stale_counter");
  }

  @Test
  public void bindings_registersHotspotCollectorsWhenEnabled() {
    new PrometheusModule()
        .bindings(
            Environment.simple(),
            ConfigFactory.parseMap(
                ImmutableMap.of(PrometheusModule.REGISTER_DEFAULT_HOTSPOT_COLLECTORS_KEY, true)));

    assertThat(familyNames()).contains("jvm_memory_bytes_used", "process_cpu_seconds");
  }

  @Test
  public void bindings_skipsHotspotCollectorsWhenDisabled() {
    new PrometheusModule()
        .bindings(
            Environment.simple(),
            ConfigFactory.parseMap(
                ImmutableMap.of(PrometheusModule.REGISTER_DEFAULT_HOTSPOT_COLLECTORS_KEY, false)));

    assertThat(familyNames()).isEmpty();
  }

  @Test
  public void bindings_bindsDefaultRegistry() {
    List<Binding<?>> bindings =
        new PrometheusModule().bindings(Environment.simple(), ConfigFactory.empty());

    assertThat(bindings).hasSize(1);
    assertThat(bindings.get(0).asScala().key().clazz()).isEqualTo(CollectorRegistry.class);
  }

  private static List<String> familyNames() {
    return Collections.list(CollectorRegistry.defaultRegistry.metricFamilySamples()).stream()
        .map(family -> family.name)
        .toList();
  }
}
