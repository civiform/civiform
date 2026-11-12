package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.settings.SettingsManifest;
import views.admin.ti.EditTrustedIntermediaryGroupPageView;
import views.admin.ti.EditTrustedIntermediaryGroupView;
import views.admin.ti.TrustedIntermediaryGroupIndexPageView;
import views.admin.ti.TrustedIntermediaryGroupListPageView;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.shared.LayoutDeps;

public class TrustedIntermediaryManagementControllerTest extends ResetPostgres {

  private static final String NAME_KEY = "name";
  private static final String DESCRIPTION_KEY = "description";
  private static final String EMAIL_ADDRESS_KEY = "emailAddress";
  private static final String ACCOUNT_ID_KEY = "accountId";
  private static final String ERROR_KEY = "error";
  private static final String PROVIDED_NAME_KEY = "providedName";
  private static final String PROVIDED_DESCRIPTION_KEY = "providedDescription";
  private static final String GROUP_1_NAME = "Group 1";
  private static final String GROUP_1_DESCRIPTION = "Description 1";
  private static final String GROUP_2_NAME = "Group 2";
  private static final String GROUP_2_DESCRIPTION = "Description 2";
  private static final String TEST_EMAIL = "test@example.com";

  private TrustedIntermediaryManagementController controller;
  private AccountRepository accountRepository;
  private TrustedIntermediaryGroupListPageView redesignPageView;
  private TrustedIntermediaryGroupIndexPageView indexPageView;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);

    // The Thymeleaf page views render the admin header from the current user's profile, which a
    // fake request does not carry, so give them a mocked profile.
    ProfileUtils profileUtils = mock(ProfileUtils.class);
    when(profileUtils.currentUserProfile(any())).thenReturn(mock(CiviFormProfile.class));
    LayoutDeps injectedLayoutDeps = instanceOf(LayoutDeps.class);
    LayoutDeps layoutDeps =
        new LayoutDeps(
            injectedLayoutDeps.baseViewDeps(),
            injectedLayoutDeps.bundledAssetsFinder(),
            profileUtils,
            injectedLayoutDeps.deploymentType(),
            injectedLayoutDeps.clock());

    // Spies, so tests can verify which view the flags selected; the legacy page and the Thymeleaf
    // port render much of the same text.
    redesignPageView = spy(new TrustedIntermediaryGroupListPageView(layoutDeps));
    indexPageView = spy(new TrustedIntermediaryGroupIndexPageView(layoutDeps));

    controller =
        new TrustedIntermediaryManagementController(
            instanceOf(TrustedIntermediaryGroupListView.class),
            instanceOf(EditTrustedIntermediaryGroupView.class),
            instanceOf(EditTrustedIntermediaryGroupPageView.class),
            accountRepository,
            instanceOf(FormFactory.class),
            instanceOf(SettingsManifest.class),
            redesignPageView,
            indexPageView);
  }

  @Test
  public void index_withNoTis_displaysEmptyList() {
    var result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Create new trusted intermediary");
  }

  @Test
  public void index_withTis_displaysListOfTis() {
    accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    accountRepository.createNewTrustedIntermediaryGroup(GROUP_2_NAME, GROUP_2_DESCRIPTION);

    var result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains(GROUP_1_NAME);
    assertThat(contentAsString(result)).contains(GROUP_2_NAME);
  }

  @Test
  public void index_thymeleafDisabled_rendersLegacyView() {
    var result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    verify(indexPageView, never()).render(any(), any());
    verify(redesignPageView, never()).render(any(), any());
  }

  @Test
  public void index_thymeleafAndRedesignEnabled_rendersRedesignView() {
    var result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .addCiviFormSetting("ADMIN_UI_MIGRATION_UX_REDESIGN_SC_ENABLED", "true")
                .build());

    assertThat(result.status()).isEqualTo(OK);
    verify(redesignPageView).render(any(), any());
    verify(indexPageView, never()).render(any(), any());
  }

  @Test
  public void index_thymeleafEnabled_withNoTis_rendersThymeleafViewWithEmptyList() {
    var result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    verify(indexPageView).render(any(), any());
    assertThat(contentAsString(result)).contains("Create new trusted intermediary");
    assertThat(contentAsString(result)).doesNotContain("cf-ti-row");
  }

  @Test
  public void index_thymeleafEnabled_withTis_rendersSortableRows() {
    accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    accountRepository.createNewTrustedIntermediaryGroup(GROUP_2_NAME, GROUP_2_DESCRIPTION);

    var result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .build());
    String content = contentAsString(result);

    assertThat(result.status()).isEqualTo(OK);
    verify(indexPageView).render(any(), any());
    assertThat(content).contains(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    assertThat(content).contains(GROUP_2_NAME, GROUP_2_DESCRIPTION);
    // The ids, classes and data attributes admin_trusted_intermediary_list.ts sorts by.
    assertThat(content).contains("id=\"cf-ti-list\"", "cf-ti-sublist", "cf-ti-element");
    assertThat(content).contains("data-tiname=\"" + GROUP_1_NAME + "\"", "data-nummember=\"0\"");
  }

  @Test
  public void index_thymeleafEnabled_withFlashError_showsNonDismissibleErrorToastAndStickyValues() {
    var result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .flash(
                    ImmutableMap.of(
                        ERROR_KEY,
                        "Must provide a unique group name.",
                        PROVIDED_NAME_KEY,
                        GROUP_1_NAME,
                        PROVIDED_DESCRIPTION_KEY,
                        GROUP_1_DESCRIPTION))
                .build());
    String content = contentAsString(result);

    assertThat(result.status()).isEqualTo(OK);
    verify(indexPageView).render(any(), any());
    assertThat(content)
        .contains(
            "id=\"warning-message-ti-form-fill\"",
            "canDismiss=\"false\"",
            "Error: Must provide a unique group name.");
    assertThat(content)
        .contains("value=\"" + GROUP_1_NAME + "\"", "value=\"" + GROUP_1_DESCRIPTION + "\"");
  }

  @Test
  public void create_withValidData_createsNewTiGroup() {
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of(NAME_KEY, GROUP_1_NAME, DESCRIPTION_KEY, GROUP_1_DESCRIPTION))
            .build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(accountRepository.listTrustedIntermediaryGroups()).hasSize(1);
    assertThat(accountRepository.listTrustedIntermediaryGroups().get(0).getName())
        .isEqualTo(GROUP_1_NAME);
  }

  @Test
  public void create_withoutName_showsErrorMessage() {
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of(DESCRIPTION_KEY, GROUP_1_DESCRIPTION))
            .build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(ERROR_KEY)).contains("Must provide group name.");
  }

  @Test
  public void create_withoutDescription_showsErrorMessage() {
    var request = fakeRequestBuilder().bodyForm(ImmutableMap.of(NAME_KEY, GROUP_1_NAME)).build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(ERROR_KEY)).contains("Must provide group description.");
  }

  @Test
  public void create_withoutUniqueName_showsErrorMessage() {
    accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of(NAME_KEY, GROUP_1_NAME, DESCRIPTION_KEY, GROUP_1_DESCRIPTION))
            .build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(ERROR_KEY)).contains("Must provide a unique group name.");
  }

  @Test
  public void edit_withValidId_displaysGroupDetails() {
    var group =
        accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    accountRepository.addTrustedIntermediaryToGroup(group.id, TEST_EMAIL);

    var result = controller.edit(group.id, fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    // TODO(#10097): Assert that the title of the group is shown
    assertThat(contentAsString(result)).contains(TEST_EMAIL);
  }

  @Test
  public void edit_withInvalidId_returnsNotFound() {
    var result = controller.edit(999L, fakeRequest());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("no such group.");
  }

  @Test
  public void delete_withValidId_deletesGroup() {
    var group =
        accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);

    var result = controller.delete(group.id, fakeRequest());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(
            accountRepository.listTrustedIntermediaryGroups().stream()
                .map(g -> g.id)
                .filter(id -> id.equals(group.id)))
        .isEmpty();
  }

  @Test
  public void delete_withInvalidId_returnsNotFound() {
    var result = controller.delete(999L, fakeRequest());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("no such group");
  }

  @Test
  public void addIntermediary_withValidData_addsIntermediary() {
    var group =
        accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of(EMAIL_ADDRESS_KEY, TEST_EMAIL)).build();

    var result = controller.addIntermediary(group.id, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(
            accountRepository
                .getTrustedIntermediaryGroup(group.id)
                .get()
                .getTrustedIntermediaries()
                .stream()
                .map(AccountModel::getEmailAddress)
                .toList())
        .contains(TEST_EMAIL);
  }

  @Test
  public void addIntermediary_withInvalidGroupId_showsErrorMessage() {
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of(EMAIL_ADDRESS_KEY, TEST_EMAIL)).build();

    var result = controller.addIntermediary(999L, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(ERROR_KEY)).contains("No such TI group.");
  }

  @Test
  public void removeIntermediary_withValidData_removesIntermediary() {
    var group =
        accountRepository.createNewTrustedIntermediaryGroup(GROUP_1_NAME, GROUP_1_DESCRIPTION);
    accountRepository.addTrustedIntermediaryToGroup(group.id, TEST_EMAIL);
    var ti =
        accountRepository
            .getTrustedIntermediaryGroup(group.id)
            .get()
            .getTrustedIntermediaries()
            .get(0);
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of(ACCOUNT_ID_KEY, ti.id.toString())).build();

    var result = controller.removeIntermediary(group.id, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(
            accountRepository
                .getTrustedIntermediaryGroup(group.id)
                .get()
                .getTrustedIntermediaries()
                .stream()
                .map(AccountModel::getEmailAddress)
                .toList())
        .doesNotContain(TEST_EMAIL);
  }

  @Test
  public void removeIntermediary_withInvalidGroupId_showsErrorMessage() {
    AccountModel ti = new AccountModel();
    ti.setEmailAddress(TEST_EMAIL);
    ti.save();
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of(ACCOUNT_ID_KEY, ti.id.toString())).build();

    var result = controller.removeIntermediary(999L, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(ERROR_KEY)).contains("No such TI group.");
  }
}
