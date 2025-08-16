package modules;

import com.google.inject.AbstractModule;
import services.SettingsCacheProvider;
import services.settings.SettingsCache;

public class SettingsCacheModule extends AbstractModule {
  @Override
  protected void configure() {
    // asEagerSingleton() makes Guice instantiate SettingsCache at startup
    bind(SettingsCache.class).toProvider(SettingsCacheProvider.class).asEagerSingleton();
  }
}
