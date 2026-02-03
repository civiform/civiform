package views.dev;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;
import views.components.SelectWithLabel;

@Data
@Builder
public class DevToolsPageViewModel implements BaseViewModel {
  private final Optional<String> maybeFlash;
  private final String seedProgramsUrl;
  private final String seedQuestionsUrl;
  private final String clearUrl;
  private final String clearCacheUrl;
  private final String runDurableJobUrl;
  private final String iconsUrl;
  private final String homeUrl;
  private final String addressToolsUrl;
  private final String sessionProfileUrl;
  private final String sessionDisplayUrl;
  private final boolean isDev;
  private final ImmutableList<SelectWithLabel.OptionValue> durableJobOptions;
  private final String csrfToken;
}
