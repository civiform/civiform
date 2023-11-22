package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.ApplicantModel;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Lang;
import play.i18n.Langs;
import repository.AccountRepository;
import services.LocalizedStrings;
import services.settings.SettingsManifest;

public class LanguageUtilsTest extends WithMockedProfiles {
  @Test
  public void testMultipleLanguages() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables())
        .thenReturn(ImmutableList.of(Lang.forCode("en-US"), Lang.forCode("ko")));
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);

    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
  }

  @Test
  public void testOneLanguageSetDefault() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("ko")));
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.KOREAN);
  }

  @Test
  public void testNoLanguageSetDefaultUs() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale())
        .isEqualTo(LocalizedStrings.DEFAULT_LOCALE);
  }

  @Test
  public void testNoLanguageKeepPreferredLanguageIfSet() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.KOREAN);
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.KOREAN);
  }

  @Test
  public void testOneLanguageKeepPreferredLanguageIfSet() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.KOREAN);
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.KOREAN);
  }

  @Test
  public void getApplicantEnabledLanguages_returns_filtered_list() {
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables())
        .thenReturn(
            ImmutableList.of(Lang.forCode("en-US"), Lang.forCode("es-US"), Lang.forCode("vi")));

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getCiviformApplicantEnabledLanguages())
        .thenReturn(Optional.of(ImmutableList.of("en-US", "vi")));

    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);

    assertThat(languageUtils.getApplicantEnabledLanguages().size()).isEqualTo(2);
  }

  @Test
  public void getApplicantEnabledLanguages_returns_all_system_supported_lanaguages() {
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables())
        .thenReturn(
            ImmutableList.of(Lang.forCode("en-US"), Lang.forCode("es-US"), Lang.forCode("vi")));

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getCiviformApplicantEnabledLanguages())
        .thenReturn(Optional.of(ImmutableList.of()));

    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);

    assertThat(languageUtils.getApplicantEnabledLanguages().size()).isEqualTo(3);
  }

  @Test
  public void getApplicantEnabledLanguages_does_not_return_unsupported_system_languages() {
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables())
        .thenReturn(
            ImmutableList.of(Lang.forCode("en-US"), Lang.forCode("es-US"), Lang.forCode("vi")));

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getCiviformApplicantEnabledLanguages())
        .thenReturn(Optional.of(ImmutableList.of("en-US", "vi", "jp")));

    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);

    var applicantEnabledLanguages = languageUtils.getApplicantEnabledLanguages();

    assertThat(applicantEnabledLanguages.size()).isEqualTo(2);
    assertThat(
            applicantEnabledLanguages.stream()
                .filter(x -> x.code().equals("jp"))
                .findAny()
                .isEmpty())
        .isEqualTo(true);
  }
}
