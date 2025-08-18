package modules;

import com.google.inject.AbstractModule;
import services.settings.FakeSettingsCache;
import services.settings.SettingsGetter;

public class FakeSettingsCacheModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SettingsGetter.class).to(FakeSettingsCache.class);
  }
}
