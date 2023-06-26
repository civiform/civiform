package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.ApplicantAuthProviderName;
import annotations.BindingAnnotations.EnUsLang;
import annotations.BindingAnnotations.Now;
import com.github.slugify.Slugify;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import services.program.ProgramService;
import services.program.ProgramServiceImpl;

/**
 * This class is a Guice module that tells Guice how to bind several different types. This Guice
 * module is created when the Play application starts.
 *
 * <p>Play will automatically use any class called `Module` that is in the root package of the
 * project. You can create modules in other locations, such as the "modules" folder, by adding
 * `play.modules.enabled` settings to the `application.conf` configuration file.
 */
public class MainModule extends AbstractModule {

  public static final Slugify SLUGIFIER = Slugify.builder().build();

  @Override
  public void configure() {
    bind(ProgramService.class).to(ProgramServiceImpl.class);
  }

  @Provides
  @EnUsLang
  public Messages provideEnUsMessages(MessagesApi messagesApi) {
    return messagesApi.preferred(ImmutableList.of(Lang.forCode("en-US")));
  }

  @Provides
  @Now
  public LocalDateTime provideNow(Clock clock) {
    return LocalDateTime.now(clock);
  }

  @Provides
  public Clock provideClock(ZoneId zoneId) {
    // Use the system clock as the default implementation of Clock.
    return Clock.system(zoneId);
  }

  @Provides
  public ZoneId provideZoneId(Config config) {
    return ZoneId.of(checkNotNull(config).getString("civiform_time_zone_id"));
  }

  @Provides
  @ApplicantAuthProviderName
  public String provideApplicantAuthProviderName(Config config) {
    checkNotNull(config);

    boolean applicantPortalNamePresent =
        config.hasPath("applicant_portal_name")
            && !config.getString("applicant_portal_name").isBlank();

    boolean civicEntityNameIsPresent =
        config.hasPath("whitelabel_civic_entity_full_name")
            && !config.getString("whitelabel_civic_entity_full_name").isBlank();

    if (applicantPortalNamePresent) {
      return config.getString("applicant_portal_name");
    } else if (civicEntityNameIsPresent) {
      return config.getString("whitelabel_civic_entity_full_name");
    }

    return "TestPortal";
  }
}
