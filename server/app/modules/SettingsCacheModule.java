package modules;

import com.google.inject.AbstractModule;
import services.settings.SettingsCache;
import services.settings.SettingsGetter;

public class SettingsCacheModule extends AbstractModule {
  @Override
  protected void configure() {
    // asEagerSingleton() makes Guice instantiate SettingsCache at startup
    bind(SettingsGetter.class).to(SettingsCache.class).asEagerSingleton();
  }
}
