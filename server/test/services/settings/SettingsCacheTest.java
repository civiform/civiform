package services.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.SettingsGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.inject.ApplicationLifecycle;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import repository.SettingsGroupRepository;

public class SettingsCacheTest extends ResetPostgres {

  public static final String TEST_AUTHORITY_ID = "test-id";
  private SettingsService settingsService;
  private SettingsCache cache;
  private CiviFormProfile testProfile;

  @Before
  public void setUp() {
    testProfile = mock(CiviFormProfile.class);
    when(testProfile.getAuthorityId())
        .thenReturn(CompletableFuture.completedFuture(TEST_AUTHORITY_ID));
    settingsService = instanceOf(SettingsService.class);
    cache =
        new SettingsCache(
            instanceOf(SettingsGroupRepository.class),
            instanceOf(DatabaseExecutionContext.class),
            instanceOf(ApplicationLifecycle.class));
  }

  @Test
  public void get_noneYet_returnsEmpty() {
    // We rely on the asynchronous nature of the initial load from the DB to test the initial empty
    // state of the cache.
    assertThat(cache.get()).isEmpty();
  }

  @Test
  public void get_waitForInitialLoad_returnsInitialSettings() throws Exception {
    // Wait for the initial load on class constructions
    Thread.sleep(500);

    Optional<SettingsGroupModel> settings = cache.get();

    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings().get("APPLICANT_PORTAL_NAME")).isEqualTo("TestPortal");
  }

  @Test
  public void get_notified_returnsNewSettings() throws Exception {
    // Wait for the initial load on class constructions
    Thread.sleep(500);
    Connection conn = DB.getDefault().dataSource().getConnection();
    Statement stmt = conn.createStatement();
    Optional<SettingsGroupModel> settings = cache.get();

    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings().get("APPLICANT_PORTAL_NAME")).isEqualTo("TestPortal");

    // Update a known setting value
    assertThat(
            settingsService
                .updateSettings(
                    ImmutableMap.of("APPLICANT_PORTAL_NAME", "aNewTestInterface"), testProfile)
                .updated())
        .isTrue();
    stmt.executeUpdate("NOTIFY " + SettingsCache.CHANNEL + ", 'id'");
    Thread.sleep(500); // Fails without this wait, since the reloadFromDb is async

    settings = cache.get();

    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings().get("APPLICANT_PORTAL_NAME"))
        .isEqualTo("aNewTestInterface");
  }
}
