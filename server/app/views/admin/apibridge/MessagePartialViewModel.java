package views.admin.apibridge;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import services.AlertType;
import views.admin.BaseViewModel;

/** Contains all the properties for rendering the MessagePartial.html */
public record MessagePartialViewModel(AlertType alertType, ImmutableList<String> messages)
    implements BaseViewModel {
  public String alertTypeName() {
    return StringUtils.capitalize(alertType().name().toLowerCase(Locale.ROOT));
  }
}
