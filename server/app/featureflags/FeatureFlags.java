package featureflags;

import static com.google.common.base.Preconditions.checkNotNull;
import static featureflags.FeatureFlag.ADMIN_REPORTING_UI_ENABLED;

import com.google.common.collect.ImmutableSortedMap;
import com.typesafe.config.Config;
import java.util.Comparator;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http.Request;

/**
 * Provides configuration backed values that indicate if application wide features are enabled.
 *
 * <p>Values are primarily derived from {@link Config} with overrides allowed via the {@link
 * Request} session cookie as set by {@link controllers.dev.FeatureFlagOverrideController}.
 */
public final class FeatureFlags {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlags.class);
  private final Config config;

  @Inject
  FeatureFlags(Config config) {
    this.config = checkNotNull(config);
  }

  public boolean areOverridesEnabled() {
    return config.hasPath(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString())
        && config.getBoolean(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString());
  }

  /** If the reporting view in the admin UI is enabled */
  // TODO(MichaelZetune): remove and have clients call getFlagEnabled directly.
  public boolean isAdminReportingUiEnabled() {
    return config.getBoolean(ADMIN_REPORTING_UI_ENABLED.toString());
  }

  public ImmutableSortedMap<FeatureFlag, Boolean> getAllFlagsSorted(Request request) {
    ImmutableSortedMap.Builder<FeatureFlag, Boolean> map =
        ImmutableSortedMap.orderedBy(Comparator.comparing(FeatureFlag::toString));

    for (FeatureFlag flag : FeatureFlag.values()) {
      map.put(flag, getFlagEnabled(request, flag));
    }

    return map.build();
  }

  /**
   * Returns the current setting for {@code flag} from {@link Config} if present, allowing for an
   * overriden value from the session cookie.
   *
   * <p>Returns false if the value is not present.
   */
  public boolean getFlagEnabled(Request request, FeatureFlag flag) {
    Optional<Boolean> maybeConfigValue = getFlagEnabledFromConfig(flag);
    if (maybeConfigValue.isEmpty()) {
      return false;
    }
    Boolean configValue = maybeConfigValue.get();

    if (!areOverridesEnabled()) {
      return configValue;
    }

    Optional<Boolean> sessionValue =
        request.session().get(flag.toString()).map(Boolean::parseBoolean);
    if (sessionValue.isPresent()) {
      logger.warn("Returning override ({}) for feature flag: {}", sessionValue.get(), flag);
      return sessionValue.get();
    }
    return configValue;
  }

  /** Returns the current setting for {@code flag} from {@link Config} if present. */
  public Optional<Boolean> getFlagEnabledFromConfig(FeatureFlag flag) {
    if (!config.hasPath(flag.toString())) {
      logger.warn("Feature flag requested for unconfigured flag: {}", flag);
      return Optional.empty();
    }
    return Optional.of(config.getBoolean(flag.toString()));
  }
}
