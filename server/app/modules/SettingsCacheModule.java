package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.db.evolutions.ApplicationEvolutions;
import repository.SettingsCacheMaintainer;

public class SettingsCacheModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(SettingsCacheModule.class);

  @Override
  protected void configure() {

    // asEagerSingleton() makes Guice instantiate SettingsCache at startup
    bind(SettingsCacheMaintainerStarter.class).asEagerSingleton();
  }

  /**
   * This class injects ApplicationEvolutions and checks the `upToDate` method to prevent this
   * module from running until after the evolutions are completed.
   *
   * <p>See <a href="https://github.com/civiform/civiform/pull/8253">PR 8253</a> for more extensive
   * details.
   */
  public static final class SettingsCacheMaintainerStarter {

    @Inject
    public SettingsCacheMaintainerStarter(
        ApplicationEvolutions applicationEvolutions,
        Provider<SettingsCacheMaintainer> settingsCacheMaintainerProvider) {
      logger.trace("SettingsCacheMaintainerStarter - Started");

      if (applicationEvolutions.upToDate()) {
        logger.trace("SettingsCacheMaintainerStarter - Task Start after evolution");
        settingsCacheMaintainerProvider.get().init();
        logger.trace("SettingsCacheMaintainerStarter - Task End");
      } else {
        logger.trace("Evolutions Not Ready");
      }
    }
  }
}
