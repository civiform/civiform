package views.tags;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.AssetsFinder;
import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import play.Environment;

public final class CiviFormProcessorDialect extends AbstractProcessorDialect {
  private final AssetsFinder assetsFinder;
  private final Environment environment;

  public CiviFormProcessorDialect(AssetsFinder assetsFinder, Environment environment) {
    super("CiviForm", "cf", 10);
    this.assetsFinder = checkNotNull(assetsFinder);
    this.environment = checkNotNull(environment);
  }

  @Override
  public Set<IProcessor> getProcessors(String dialectPrefix) {
    return Set.of(new IconElementTagProcessor(getPrefix(), assetsFinder, environment));
  }
}
