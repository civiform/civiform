package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import java.time.Clock;
import java.time.ZoneId;
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

  @Override
  public void configure() {
    bind(ProgramService.class).to(ProgramServiceImpl.class);
    bind(QuestionService.class).to(QuestionServiceImpl.class);
    bind(ApplicantService.class).to(ApplicantServiceImpl.class);
  }

  @Provides
  public Clock provideClock(Config config) {
    ZoneId zoneId = ZoneId.of(checkNotNull(config).getString("java.time.zoneid"));
    // Use the system clock as the default implementation of Clock
    return Clock.system(zoneId);
  }
}
