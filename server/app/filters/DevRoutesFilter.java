package filters;

import static play.mvc.Results.notFound;

import com.google.inject.Inject;
import java.util.Locale;
import javax.inject.Provider;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import services.DeploymentType;

/** Ensure that all /dev routes are only accessible in dev and staging. */
public class DevRoutesFilter extends EssentialFilter {
  private boolean isDevOrStaging;

  @Inject
  public DevRoutesFilter(Provider<DeploymentType> deploymentType) {
    this.isDevOrStaging = deploymentType.get().isDevOrStaging();
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          boolean isDevPath = request.uri().toLowerCase(Locale.ROOT).startsWith("/dev");
          if (!isDevOrStaging && isDevPath) {
            return Accumulator.done(notFound());
          }
          return next.apply(request);
        });
  }
}
