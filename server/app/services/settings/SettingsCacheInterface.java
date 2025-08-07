package services.settings;

import java.util.Optional;
import models.SettingsGroupModel;

public interface SettingsCacheInterface {
  public Optional<SettingsGroupModel> get();
}
