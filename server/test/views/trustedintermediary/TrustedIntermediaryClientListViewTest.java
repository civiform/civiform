package views.trustedintermediary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.ConfigFactory;
import controllers.LanguageUtils;
import controllers.WithMockedProfiles;
import controllers.applicant.ApplicantRoutes;
import java.time.Instant;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import modules.ThymeleafModule;
import org.junit.Before;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import repository.AccountRepository;
import repository.SearchParameters;
import services.BundledAssetsFinder;
import services.DateConverter;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.FakeRequestBuilder;
import views.BaseHtmlLayout;
import views.LanguageSelector;
import views.ViewUtils;
import views.components.PageNotProductionBanner;

public class TrustedIntermediaryClientListViewTest extends WithMockedProfiles {

  private SettingsManifest settingsManifest;
  private ProfileUtils profileUtils;
  private LanguageUtils languageUtils;
  private CiviFormProfile profile;
  private Messages messages;
  private AccountRepository accountRepo;
  private TrustedIntermediaryClientListView view;

  @Before
  public void setUp() {
    resetDatabase();

    settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getCiviformImageTag()).thenReturn(Optional.of("fake-image-tag"));
    when(settingsManifest.getFaviconUrl()).thenReturn(Optional.of("favicon-url"));

    profileUtils = mock(ProfileUtils.class);
    profile = mock(CiviFormProfile.class);
    languageUtils = mock(LanguageUtils.class);
    accountRepo = instanceOf(AccountRepository.class);

    messages = mock(Messages.class);
    when(messages.at(MessageKey.CONTENT_EMAIL_TOOLTIP.getKeyName())).thenReturn("email tooltip");
    when(messages.at(MessageKey.NAME_LABEL_FIRST.getKeyName())).thenReturn("First Name Label");
    when(messages.at(MessageKey.NAME_LABEL_LAST.getKeyName())).thenReturn("Last Name Label");
    when(messages.at(MessageKey.DOB_LABEL.getKeyName())).thenReturn("DOB");

    ApplicantLayout applicantLayout =
        new ApplicantLayout(
            instanceOf(BaseHtmlLayout.class),
            instanceOf(ViewUtils.class),
            profileUtils,
            instanceOf(LanguageSelector.class),
            languageUtils,
            settingsManifest,
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class),
            instanceOf(PageNotProductionBanner.class),
            mock(MessagesApi.class),
            instanceOf(ApplicantRoutes.class),
            instanceOf(ThymeleafModule.PlayThymeleafContextFactory.class),
            instanceOf(TemplateEngine.class));

    view =
        new TrustedIntermediaryClientListView(
            applicantLayout,
            instanceOf(DateConverter.class),
            instanceOf(ProgramService.class),
            ConfigFactory.load());
  }

  @Test
  // Only the oldest Applicant should be used.
  public void render_ignoresOtherApplicants() {
    Http.Request request = FakeRequestBuilder.fakeRequest();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(profile));
    when(languageUtils.getPreferredLanguage(request)).thenReturn(Lang.defaultLang());
    when(settingsManifest.getSupportEmailAddress(request)).thenReturn(Optional.of("supportEmail"));
    when(settingsManifest.getWhitelabelCivicEntityFullName(request))
        .thenReturn(Optional.of("CE full name"));
    when(settingsManifest.getWhitelabelCivicEntityShortName(request))
        .thenReturn(Optional.of("CE short name"));

    AccountModel ti = createTIWithMockedProfile(createApplicant());
    TrustedIntermediaryGroupModel tiGroup = accountRepo.listTrustedIntermediaryGroups().get(0);

    // Check a subset of the displayed info
    final String PRIMARY_APPLICANT_NAME = "ClientFirstApplicant";
    final String PRIMARY_APPLICANT_DOB = "2000-01-01";
    final String SECONDARY_APPLICANT_NAME = "ClientSecondApplicant";
    final String SECONDARY_APPLICANT_DOB = "2012-12-12";
    var tiClientAccount =
        setupTiClientAccountWithApplicant(
            PRIMARY_APPLICANT_NAME, PRIMARY_APPLICANT_DOB, "email2123", tiGroup);
    var tiClientSecondaryApplicant =
        setTiClientApplicant(tiClientAccount, SECONDARY_APPLICANT_NAME, SECONDARY_APPLICANT_DOB);

    ApplicantPersonalInfo personalInfo =
        ApplicantPersonalInfo.ofTiPartiallyCreated(
            ApplicantPersonalInfo.Representation.builder().build());

    String html =
        view.render(
                tiGroup,
                personalInfo,
                /* managedAccounts= */ ImmutableList.of(tiClientAccount),
                /* totalPageCount= */ 1,
                /* page= */ 1,
                SearchParameters.builder().build(),
                request,
                messages,
                ti.id)
            .body();

    assertThat(html).contains(PRIMARY_APPLICANT_NAME);
    assertThat(html).contains(PRIMARY_APPLICANT_DOB);
    assertThat(html).doesNotContain(SECONDARY_APPLICANT_NAME);

    // Now make the newer Applicant older and ensure it shows instead of the
    // first to ensure the previous test was actually excluding the secondary
    // applicant.
    tiClientSecondaryApplicant.setWhenCreated(Instant.EPOCH).save();

    html =
        view.render(
                tiGroup,
                personalInfo,
                /* managedAccounts= */ ImmutableList.of(tiClientAccount),
                /* totalPageCount= */ 1,
                /* page= */ 1,
                SearchParameters.builder().build(),
                request,
                messages,
                ti.id)
            .body();
    assertThat(html).doesNotContain(PRIMARY_APPLICANT_NAME);
    assertThat(html).contains(SECONDARY_APPLICANT_NAME);
    assertThat(html).contains(SECONDARY_APPLICANT_DOB);
  }

  private AccountModel setupTiClientAccountWithApplicant(
      String firstName, String dob, String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = setupTiClientAccount(email, tiGroup);
    setTiClientApplicant(account, firstName, dob);
    return account;
  }

  private AccountModel setupTiClientAccount(String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    account.setManagedByGroup(tiGroup);
    account.save();
    return account;
  }

  private ApplicantModel setTiClientApplicant(
      AccountModel tiAccount, String firstName, String dob) {
    ApplicantModel applicant = new ApplicantModel();
    tiAccount.getApplicants().add(applicant);
    applicant.setAccount(tiAccount);
    applicant.setUserName(
        firstName,
        /* middleName= */ Optional.empty(),
        Optional.of("Last"),
        /* nameSuffix= */ Optional.empty());
    applicant.setDateOfBirth(dob);
    applicant.save();
    tiAccount.save();
    return applicant;
  }
}
