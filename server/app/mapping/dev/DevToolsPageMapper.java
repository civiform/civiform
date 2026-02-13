package mapping.dev;

import com.google.common.collect.ImmutableList;
import durablejobs.DurableJobName;
import views.dev.DevToolsPageViewModel;

/** Maps data to the DevToolsPageViewModel. */
public final class DevToolsPageMapper {

  public DevToolsPageViewModel map(boolean isDev, String csrfToken) {
    ImmutableList<String> durableJobOptions =
        ImmutableList.copyOf(DurableJobName.values()).stream()
            .map(DurableJobName::toString)
            .collect(ImmutableList.toImmutableList());

    return DevToolsPageViewModel.builder()
        .durableJobOptions(durableJobOptions)
        .isDev(isDev)
        .csrfToken(csrfToken)
        .build();
  }
}
