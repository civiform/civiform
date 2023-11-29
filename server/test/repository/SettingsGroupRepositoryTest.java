package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import models.SettingsGroupModel;
import org.junit.Before;
import org.junit.Test;

public class SettingsGroupRepositoryTest extends ResetPostgres {

  private SettingsGroupRepository settingsGroupRepository;

  @Before
  public void setUp() {
    settingsGroupRepository = instanceOf(SettingsGroupRepository.class);
  }

  @Test
  public void getCurrentSettings_returnsTheMostRecentGroup() {
    DB.getDefault().truncate(SettingsGroupModel.class);

    var groupA = new SettingsGroupModel(ImmutableMap.of("TEST", "true"), "test");
    groupA.save();
    groupA.setCreateTimeForTest("2040-01-01T00:00:00Z").save();

    var groupB = new SettingsGroupModel(ImmutableMap.of("TEST", "false"), "test");
    groupB.save();

    groupB.setCreateTimeForTest("2041-01-01T00:00:00Z").save();

    SettingsGroupModel result =
        settingsGroupRepository.getCurrentSettings().toCompletableFuture().join().get();

    assertThat(result.getSettings()).isEqualTo(groupB.getSettings());
  }
}
