package modules;

import static com.google.common.base.Preconditions.checkNotNull;

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
import services.applicant.ApplicantService;
import services.applicant.ApplicantServiceImpl;
import services.program.ProgramService;
import services.program.ProgramServiceImpl;
import services.question.QuestionService;
import services.question.QuestionServiceImpl;

/**
 * This class is a Guice module that tells Guice how to bind several different types. This Guice
 * module is created when the Play application starts.
 *
 * <p>Play will automatically use any class called `Module` that is in the root package. You can
 * create modules in other locations by adding `play.modules.enabled` settings to the
 * `application.conf` configuration file.
 */
public class MainModule extends AbstractModule {

  public static final Slugify SLUGIFIER = Slugify.builder().build();

  @Override
  public void configure() {
    bind(ProgramService.class).to(ProgramServiceImpl.class);
    bind(QuestionService.class).to(QuestionServiceImpl.class);
    bind(ApplicantService.class).to(ApplicantServiceImpl.class);
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
    return ZoneId.of(checkNotNull(config).getString("civiform.time.zoneid"));
  }
}
