package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import models.Applicant;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Lang;
import play.i18n.Langs;
import repository.UserRepository;

public class LanguageUtilsTest extends WithMockedProfiles {
  @Test
  public void testMultipleLanguages() {
    Applicant applicant = createApplicantWithMockedProfile();
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables())
        .thenReturn(ImmutableList.of(Lang.forCode("en-US"), Lang.forCode("ko")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
  }

  @Test
  public void testOneLanguageSetDefaultNonUs() {
    Applicant applicant = createApplicantWithMockedProfile();
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("ko")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.KOREA);
  }

  @Test
  public void testOneLanguageSetDefault() {
    Applicant applicant = createApplicantWithMockedProfile();
    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.US);
  }

  @Test
  public void testNoLanguageSetDefaultUs() {
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.CHINA);
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);
    applicant = languageUtils.maybeSetDefaultLocale(applicant);
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.US);
  }
}
