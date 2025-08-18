package services.settings;

import java.util.Optional;
import javax.inject.Inject;
import models.SettingsGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.SettingsGroupRepository;

public final class FakeSettingsCache implements SettingsGetter {
  private static final Logger logger = LoggerFactory.getLogger(FakeSettingsCache.class);
  private final SettingsGroupRepository repo;

  @Inject
  public FakeSettingsCache(SettingsGroupRepository repo) {
    logger.trace("Initializing the fake Settings Cache");
    this.repo = repo;
  }

  @Override
  public Optional<SettingsGroupModel> get() {
    return repo.getCurrentSettings().toCompletableFuture().join();
  }
}
