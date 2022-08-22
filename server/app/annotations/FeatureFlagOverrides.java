package annotations;

import com.typesafe.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import modules.FeatureFlagsModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A "singleton" like class that supports overriding feature flag values as otherwise managed in
 * {@link FeatureFlagsModule}.
 *
 * <p>Note: this is only intended to work in a single development/test server not a replicated
 * production service.
 */
public class FeatureFlagOverrides {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlagOverrides.class);
  // The main configuration control for if overrides are allowed.
  private static final String FEATURE_FLAG_OVERRIDES_ENABLED = "feature_flag_overrides_enabled";
  // Static storage for the overrides.
  private static final Map<String, String> overrides;
  private final Config config;

  static {
    overrides = new HashMap<>();
  }

  @Inject
  FeatureFlagOverrides(Config config) {
    this.config = config;
  }

  /** Set an arbitrary {@code value} override for an arbitrary {@code flag}. */
  public void setOverride(String flag, String value) {
    synchronized (overrides) {
      logger.warn("Overriding {} to {}", flag, value);
      overrides.put(flag, value);
    }
  }

  /**
   * Returns the override value for {@code flag} if it is present and overrides are enabled.
   *
   * <p>Parsing follows {@link Boolean#parseBoolean(String)} rules.
   */
  public Optional<Boolean> getOverrideBoolean(String flag) {
    if (!config.getBoolean(FEATURE_FLAG_OVERRIDES_ENABLED)) {
      return Optional.empty();
    }

    synchronized (overrides) {
      // Overrides shouldn't be used in prod so be verbose about them in general.
      logger.warn("Providing override for {}: {}", flag, overrides.get(flag));
      return Optional.ofNullable(overrides.get(flag)).map(Boolean::parseBoolean);
    }
  }
}
