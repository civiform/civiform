package services.settings;

import com.google.common.collect.ImmutableMap;

public abstract class AbstractSettingsManifest {

  public abstract ImmutableMap<String, SettingsSection> getSections();
}
