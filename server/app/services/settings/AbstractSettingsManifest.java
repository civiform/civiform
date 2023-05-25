package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

public abstract class AbstractSettingsManifest {

  public abstract ImmutableMap<String, SettingsSection> getSections();
}
