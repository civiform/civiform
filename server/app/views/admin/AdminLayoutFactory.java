package views.admin;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final Config configuration;
  private final FeatureFlags featureFlags;

  @Inject
  public AdminLayoutFactory(ViewUtils viewUtils, Config configuration, FeatureFlags featureFlags) {
    this.viewUtils = Preconditions.checkNotNull(viewUtils);
    this.configuration = Preconditions.checkNotNull(configuration);
    this.featureFlags = Preconditions.checkNotNull(featureFlags);
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(viewUtils, configuration, featureFlags, navPage);
  }
}
