package services.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import io.ebean.DB;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.SettingsGroup;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.test.Helpers;
import repository.ResetPostgres;
import repository.SettingsGroupRepository;

public class SettingsServiceTest extends ResetPostgres {

  public static final String TEST_AUTHORITY_ID = "test-id";
  private SettingsService settingsService;
  private static ImmutableMap<String, String> TEST_SETTINGS = ImmutableMap.of("TEST_BOOL", "true");
  private SettingsManifest testManifest =
      new SettingsManifest(
          ImmutableMap.of(
              "test section",
              SettingsSection.create(
                  "test section",
                  "test description",
                  ImmutableList.of(),
                  ImmutableList.of(
                      SettingDescription.create(
                          "TEST_BOOL",
                          "test description",
                          SettingType.BOOLEAN,
                          SettingMode.ADMIN_WRITEABLE),
                      SettingDescription.create(
                          "TEST_BOOL_READABLE",
                          "test description",
                          SettingType.BOOLEAN,
                          SettingMode.ADMIN_READABLE)))),
          ConfigFactory.parseMap(
              ImmutableMap.of("test_bool", "true", "test_bool_readable", "false")));

  private CiviFormProfile testProfile;

  @Before
  public void setUp() {
    testProfile = mock(CiviFormProfile.class);
    when(testProfile.getAuthorityId())
        .thenReturn(CompletableFuture.completedFuture(TEST_AUTHORITY_ID));

    settingsService = new SettingsService(instanceOf(SettingsGroupRepository.class), testManifest);
  }

  @Test
  public void loadSettings_returnsTheSettingsMap() {
    createTestSettings();

    var result = settingsService.loadSettings().toCompletableFuture().join().get();

    assertThat(result).isEqualTo(TEST_SETTINGS);
  }

  @Test
  public void applySettingsToRequest_addsTheSettingsToTheRequestAttributes() {
    createTestSettings();
    Http.Request request = Helpers.fakeRequest().build();

    Http.RequestHeader resultRequest =
        settingsService.applySettingsToRequest(request).toCompletableFuture().join();

    assertThat(resultRequest.attrs().get(SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY))
        .isEqualTo(TEST_SETTINGS);
  }

  @Test
  public void applySettingsToRequest_doesNotAlterAttributesIfNoSettingsFound() {
    DB.getDefault().truncate(SettingsGroup.class);
    Http.Request request = Helpers.fakeRequest().build();

    settingsService.applySettingsToRequest(request).toCompletableFuture().join();

    assertThat(request.attrs().containsKey(SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY))
        .isFalse();
  }

  @Test
  public void updateSettings_newSettingsAreDifferent_insertsANewSettingsGroup() {
    var initialSettings = settingsService.loadSettings().toCompletableFuture().join().get();

    assertThat(settingsService.updateSettings(TEST_SETTINGS, testProfile)).isTrue();
    assertThat(settingsService.loadSettings().toCompletableFuture().join().get())
        .isNotEqualTo(initialSettings);
    assertThat(getCurrentSettingsGroup().get().getCreatedBy()).isEqualTo(TEST_AUTHORITY_ID);
  }

  @Test
  public void updateSettings_newSettingsAreTheSame_doesNotinsertANewSettingsGroup() {
    var initialSettings = settingsService.loadSettings().toCompletableFuture().join().get();

    assertThat(settingsService.updateSettings(initialSettings, testProfile)).isFalse();
    assertThat(settingsService.loadSettings().toCompletableFuture().join().get())
        .isEqualTo(initialSettings);
  }

  @Test
  public void
      migrateConfigValuesToSettingsGroup_migratesSettingsFromHoconUnlessNoChangesAreFound() {
    DB.getDefault().truncate(SettingsGroup.class);

    assertThat(getCurrentSettingsGroup()).isEmpty();
    settingsService.migrateConfigValuesToSettingsGroup();

    SettingsGroup settings = getCurrentSettingsGroup().get();
    assertThat(settings.getSettings()).isEqualTo(TEST_SETTINGS);

    settingsService.migrateConfigValuesToSettingsGroup();
    SettingsGroup settingsAfterSecondMigration = getCurrentSettingsGroup().get();
    assertThat(settingsAfterSecondMigration.id).isEqualTo(settings.id);
  }

  private Optional<SettingsGroup> getCurrentSettingsGroup() {
    return instanceOf(SettingsGroupRepository.class)
        .getCurrentSettings()
        .toCompletableFuture()
        .join();
  }

  private void createTestSettings() {
    // Since ResetPostres#resetTables create a settings group as well, if this
    // is created too fast it can attempt to create two settings group with the
    // same timestamp. Delaying by a millisecond prevents that.
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    new SettingsGroup(TEST_SETTINGS, "test").save();
  }
}
