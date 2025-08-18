package services.settings;

import java.util.Optional;
import models.SettingsGroupModel;

public interface SettingsGetter {
  public Optional<SettingsGroupModel> get();
}
