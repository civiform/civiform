package services.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import io.ebean.Database;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import models.SettingsGroupModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import play.inject.ApplicationLifecycle;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import repository.SettingsGroupRepository;

public class SettingsCacheTest extends ResetPostgres {
  private static final String TEST_AUTHORITY_ID = "test-id";
  private static final String SETTING_KEY = "APPLICANT_PORTAL_NAME";
  private static final String SETTING_VALUE = "TestPortal";
  private static final String SETTING_VALUE_NEW = "aNewTestInterface";
  private static final PGNotification[] NOTIFICATIONS =
      new PGNotification[] {
        new PGNotification() {
          @Override
          public String getName() {
            return SettingsCache.CHANNEL;
          }

          @Override
          public int getPID() {
            return 0;
          }

          @Override
          public String getParameter() {
            return "";
          }
        }
      };

  // Objects with which tests interact
  private SettingsService settingsService;
  private SettingsCache cache;

  // Mocks and mock-related fields
  private AutoCloseable mocks;
  // This captor will grab the stop hook lambda so we can call it in tearDown.
  @Captor private ArgumentCaptor<Callable<CompletableFuture<?>>> stopHookCaptor;
  @Mock private ApplicationLifecycle lifecycle;
  // Mock the entire chain of calls to get a PGConnection
  @Mock private Database database;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private Statement statement;
  @Mock private PGConnection pgConnection;

  @Before
  public void setUp() throws SQLException {
    mocks = MockitoAnnotations.openMocks(this);
    // Mock the entire chain of calls to get a PGConnection
    when(database.dataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
    // We want getNotifications() to return null until after we've changed a setting value.
    when(pgConnection.getNotifications(anyInt())).thenReturn(null);

    // Capture the stop hook to be used in tearDown
    doNothing().when(lifecycle).addStopHook(stopHookCaptor.capture());

    settingsService = instanceOf(SettingsService.class);
    cache =
        new SettingsCache(
            instanceOf(SettingsGroupRepository.class),
            instanceOf(DatabaseExecutionContext.class),
            database,
            lifecycle);

    cache.init();
  }

  @After
  public void tearDown() throws Exception {
    // The ApplicationLifecycle stop hook is responsible for stopping the listener thread.
    // We invoke the captured hook here to ensure the thread is stopped cleanly and
    // doesn't interfere with other tests.
    if (stopHookCaptor.getValue() != null) {
      // This calls running=false and listenerThread.interrupt()
      stopHookCaptor.getValue().call().get();
    }
    mocks.close();
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
    Thread.sleep(100);

    Optional<SettingsGroupModel> settings = cache.get();

    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings().get(SETTING_KEY)).isEqualTo(SETTING_VALUE);
  }

  @Test
  public void get_notified_returnsNewSettings() throws Exception {
    // Wait for the initial load on class constructions
    Thread.sleep(100);

    // Update a known setting value
    assertThat(
            settingsService
                .updateSettings(ImmutableMap.of(SETTING_KEY, SETTING_VALUE_NEW), TEST_AUTHORITY_ID)
                .updated())
        .isTrue();
    Thread.sleep(100);
    Optional<SettingsGroupModel> settings = cache.get();

    // Check that we still have the old value in the cache
    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings().get(SETTING_KEY)).isEqualTo(SETTING_VALUE);

    // Now send a notification of the update
    when(pgConnection.getNotifications(anyInt())).thenReturn(NOTIFICATIONS);
    Thread.sleep(100);
    settings = cache.get();

    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings().get(SETTING_KEY)).isEqualTo(SETTING_VALUE_NEW);
  }
}
