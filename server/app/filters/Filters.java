package filters;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.pac4j.play.filters.SecurityFilter;
import play.api.http.EnabledFilters;
import play.http.DefaultHttpFilters;
import play.mvc.EssentialFilter;

public class Filters extends DefaultHttpFilters {

  @Inject
  public Filters(EnabledFilters enabledFilters, SecurityFilter securityFilter) {
    super(combine(enabledFilters.asJava().getFilters(), securityFilter.asJava()));
  }

  private static List<EssentialFilter> combine(
      List<EssentialFilter> filters, EssentialFilter toAppend) {
    List<EssentialFilter> combinedFilters = new ArrayList<>(filters);
    combinedFilters.add(toAppend);
    return combinedFilters;
  }
}
