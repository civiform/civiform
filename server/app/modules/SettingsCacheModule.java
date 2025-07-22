package modules;

import com.google.inject.AbstractModule;
import services.settings.SettingsCache;

public class SettingsCacheModule extends AbstractModule {
  @Override
  protected void configure() {
    // asEagerSingleton() makes Guice instantiate SettingsCache at startup
    bind(SettingsCache.class).asEagerSingleton();
  }
}
