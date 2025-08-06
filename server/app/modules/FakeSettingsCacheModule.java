package modules;

import com.google.inject.AbstractModule;
import services.settings.FakeSettingsCache;
import services.settings.SettingsCacheInterface;

public class FakeSettingsCacheModule extends AbstractModule {
  @Override
  protected void configure() {
    // asEagerSingleton() makes Guice instantiate SettingsCache at startup
    bind(SettingsCacheInterface.class).to(FakeSettingsCache.class).asEagerSingleton();
  }
}
