package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.ApplicantModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.mvc.Http;
import repository.AccountRepository;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.settings.SettingsManifest;

public final class LanguageUtils {
  private final AccountRepository accountRepository;
  private final Langs langs;
  private final SettingsManifest settingsManifest;
  private final MessagesApi messagesApi;
  private static final Logger LOGGER = LoggerFactory.getLogger(LanguageUtils.class);

  @Inject
  public LanguageUtils(
      AccountRepository accountRepository,
      Langs langs,
      SettingsManifest settingsManifest,
      MessagesApi messagesApi) {
    this.accountRepository = checkNotNull(accountRepository);
    this.langs = checkNotNull(langs);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public ApplicantModel maybeSetDefaultLocale(ApplicantModel applicant) {
    ApplicantData data = applicant.getApplicantData();
    if (data.hasPreferredLocale() || langs.availables().size() > 1) {
      return applicant;
    }
    data.setPreferredLocale(
        langs.availables().isEmpty()
            ? LocalizedStrings.DEFAULT_LOCALE
            : langs.availables().get(0).toLocale());
    accountRepository.updateApplicant(applicant).toCompletableFuture().join();
    return applicant;
  }

  /** Returns the languages available for an applicant to select */
  public ImmutableList<Lang> getApplicantEnabledLanguages() {
    List<Lang> allLanguages = langs.availables();
    HashSet<String> applicantLanguages =
        new HashSet<>(
            settingsManifest.getCiviformApplicantEnabledLanguages().orElse(ImmutableList.of()));

    // No overrides so use all languages
    if (applicantLanguages.isEmpty()) {
      return ImmutableList.copyOf(allLanguages);
    }

    // Check and log warning if there are languages listed for applicant that are not supported by
    // the system
    String unconfiguredLanguages =
        String.join(
            ", ",
            Sets.difference(
                applicantLanguages,
                allLanguages.stream().map(Lang::code).collect(Collectors.toSet())));

    if (!unconfiguredLanguages.isEmpty()) {
      LOGGER.warn(
          "Settings for CIVIFORM_APPLICANT_ENABLED_LANGUAGES contains one or more languages not"
              + " support by the system in CIVIFORM_SUPPORTED_LANGUAGES. Unsupported language"
              + " codes: {}.",
          unconfiguredLanguages);
    }

    // Filter list
    return allLanguages.stream()
        .filter(lang -> applicantLanguages.contains(lang.code()))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns the selected preferred language based on the applicant's browser settings. If the
   * current browser settings are for a language that is not supported/enabled for applicants it
   * returns the default system language.
   */
  public Lang getPreferredLanguage(Http.RequestHeader request) {
    var preferredLanguageCode = messagesApi.preferred(request).lang().code();

    var preferredLanguage =
        getApplicantEnabledLanguages().stream()
            .filter(lang -> lang.code().equals(preferredLanguageCode))
            .findFirst();

    return preferredLanguage.orElse(Lang.defaultLang());
  }
}
