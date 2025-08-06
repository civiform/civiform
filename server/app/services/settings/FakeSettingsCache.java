package services.settings;

import java.util.Optional;
import javax.inject.Inject;
import models.SettingsGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.SettingsGroupRepository;

public final class FakeSettingsCache implements SettingsCacheInterface {
  private static final Logger logger = LoggerFactory.getLogger(FakeSettingsCache.class);
  private final SettingsGroupRepository repo;

  @Inject
  public FakeSettingsCache(SettingsGroupRepository repo) {
    logger.warn("FAKE CACHE INIT");
    this.repo = repo;
  }

  @Override
  public Optional<SettingsGroupModel> get() {
    return repo.getCurrentSettings().toCompletableFuture().join();
  }
}
