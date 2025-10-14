package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.ebean.DB;
import io.ebean.Database;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
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
import org.postgresql.util.PSQLException;
import play.Application;
import play.ApplicationLoader;
import play.Environment;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.inject.ApplicationLifecycle;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;
import play.test.Helpers;

public class SettingsCacheMaintainerTest {
  private static final String TEST_AUTHORITY_ID = "test-id";
  private static final String SETTING_KEY = "APPLICANT_PORTAL_NAME";
  private static final String SETTING_VALUE = "TestPortal";
  private static final PGNotification[] NOTIFICATIONS =
      new PGNotification[] {
        new PGNotification() {
          @Override
          public String getName() {
            return SettingsCacheMaintainer.CHANNEL;
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
  @Inject
  @NamedCache("civiform-settings")
  private SyncCacheApi cache;

  @Inject private Application application;
  @Inject private SettingsGroupRepository repo;
  private SettingsCacheMaintainer cacheMaintainer;
  private Injector injector;

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
    // Most test classes extend ResetPostgres which accesses an injector via the application
    // provided fakeApplication(). That injector is a play.inject.Injector which struggles to inject
    // things that are annotated with annotations that take arguments, such as
    // @NamedCache("civiform-settings'). Guice makes that fairly easy, hence this test class uses
    // the guice injector directly.
    GuiceApplicationBuilder builder =
        new GuiceApplicationLoader().builder(new ApplicationLoader.Context(Environment.simple()));
    injector = Guice.createInjector(builder.applicationModule());
    injector.injectMembers(this);

    Helpers.start(application);

    DB.getDefault().truncate("civiform_settings");

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

    cacheMaintainer = new SettingsCacheMaintainer(repo, database, lifecycle);
    cacheMaintainer.init();
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
  public void notNotified_cacheIsNotCleared() throws Exception {
    // Set a known setting value
    cache.set(
        SettingsGroupRepository.CURRENT_SETTINGS_CACHE_KEY,
        new SettingsGroupModel(ImmutableMap.of(SETTING_KEY, SETTING_VALUE), TEST_AUTHORITY_ID));

    // Verify that the cache object remains unchanged
    Optional<SettingsGroupModel> settings =
        cache.get(SettingsGroupRepository.CURRENT_SETTINGS_CACHE_KEY);
    assertThat(settings).isNotEmpty();
    assertThat(settings.get().getSettings()).containsExactly(Map.entry(SETTING_KEY, SETTING_VALUE));
  }

  @Test
  public void notified_clearsCache() throws Exception {
    // Set a known setting value
    cache.set(
        SettingsGroupRepository.CURRENT_SETTINGS_CACHE_KEY,
        new SettingsGroupModel(ImmutableMap.of(SETTING_KEY, SETTING_VALUE), TEST_AUTHORITY_ID));

    Thread.sleep(100);

    // Now send a notification of the update
    when(pgConnection.getNotifications(anyInt())).thenReturn(NOTIFICATIONS);
    Thread.sleep(100);

    // Verify that the cache is cleared
    Optional<SettingsGroupModel> settings =
        cache.get(SettingsGroupRepository.CURRENT_SETTINGS_CACHE_KEY);
    assertThat(settings).isEmpty();
  }

  @Test
  public void reconnects_clearsCache() throws Exception {
    // Set a known setting value
    cache.set(
        SettingsGroupRepository.CURRENT_SETTINGS_CACHE_KEY,
        new SettingsGroupModel(ImmutableMap.of(SETTING_KEY, SETTING_VALUE), TEST_AUTHORITY_ID));

    Thread.sleep(100);

    // Now cause a disconnect, which will trigger a reconnect and cache clearing.
    when(pgConnection.getNotifications(anyInt()))
        .thenThrow(
            new PSQLException("Connection failed", null, new SocketException("Socket closed")));
    Thread.sleep(100);

    // Verify that the cache is cleared when the listener reconnects
    Optional<SettingsGroupModel> settings =
        cache.get(SettingsGroupRepository.CURRENT_SETTINGS_CACHE_KEY);
    assertThat(settings).isEmpty();
  }
}
