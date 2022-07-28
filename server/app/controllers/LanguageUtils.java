package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import javax.inject.Inject;
import models.Applicant;
import play.i18n.Langs;
import repository.UserRepository;
import services.LocalizedStrings;
import services.applicant.ApplicantData;

public final class LanguageUtils {
  private final UserRepository userRepository;
  private final Langs langs;

  @Inject
  public LanguageUtils(UserRepository userRepository, Langs langs) {
    this.userRepository = checkNotNull(userRepository);
    this.langs = checkNotNull(langs);
  }

  public Applicant maybeSetDefaultLocale(Applicant applicant) {
    ApplicantData data = applicant.getApplicantData();
    if (data.hasPreferredLocale() || langs.availables().size() > 1) {
      return applicant;
    }
    data.setPreferredLocale(
        langs.availables().isEmpty() ? LocalizedStrings.DEFAULT_LOCALE : langs.availables().get(0).toLocale());
    userRepository.updateApplicant(applicant).toCompletableFuture().join();
    return applicant;
  }
}
