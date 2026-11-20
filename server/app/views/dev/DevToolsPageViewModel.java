package views.dev;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

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
  @Builder.Default private final Optional<String> flashSuccess = Optional.empty();
  @Builder.Default private final Optional<String> flashError = Optional.empty();
  @Builder.Default private final Optional<String> flashWarning = Optional.empty();
}
