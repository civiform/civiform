package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import java.net.URI;
import play.Environment;
import services.geo.esri.EsriClient;

public class EsriModule extends AbstractModule {
  private static final String FAKE_ESRI_CLIENT_CLASS_NAME = "services.geo.esri.FakeEsriClient";
  private static final String REAL_ESRI_CLIENT_CLASS_NAME = "services.geo.esri.RealEsriClient";

  private final Environment environment;
  private final Config config;

  public EsriModule(Environment environment, Config config) {
    this.environment = checkNotNull(environment);
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    String host = URI.create(config.getString("base_url")).getHost();
    ImmutableSet<String> acceptedHosts = ImmutableSet.of("localhost", "civiform");
    boolean useFakeEsriClient =
        acceptedHosts.stream()
            .anyMatch(
                acceptedHost -> host.equals(acceptedHost) || host.startsWith(acceptedHost + ":"));
    String className =
        useFakeEsriClient ? FAKE_ESRI_CLIENT_CLASS_NAME : REAL_ESRI_CLIENT_CLASS_NAME;
    try {
      Class<? extends EsriClient> bindingClass =
          environment.classLoader().loadClass(className).asSubclass(EsriClient.class);
      bind(EsriClient.class).to(bindingClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(String.format("Failed to load esri client class: %s", className));
    }
  }
}
