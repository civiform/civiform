package views.dev;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class DevToolsPageViewModel implements BaseViewModel {
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
  private final ImmutableList<String> durableJobOptions;
  private final String csrfToken;
}
