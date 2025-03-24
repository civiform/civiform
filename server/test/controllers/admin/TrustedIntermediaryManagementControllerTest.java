package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import repository.AccountRepository;
import repository.ResetPostgres;
import views.admin.ti.EditTrustedIntermediaryGroupView;
import views.admin.ti.TrustedIntermediaryGroupListView;

public class TrustedIntermediaryManagementControllerTest extends ResetPostgres {

  private static final String NAME_KEY = "name";
  private static final String DESCRIPTION_KEY = "description";
  private static final String EMAIL_ADDRESS_KEY = "emailAddress";
  private static final String ACCOUNT_ID_KEY = "accountId";
  private static final String ERROR_KEY = "error";
  private static final String GROUP_1_NAME = "Group 1";
  private static final String GROUP_1_DESCRIPTION = "Description 1";
  private static final String GROUP_2_NAME = "Group 2";
  private static final String GROUP_2_DESCRIPTION = "Description 2";
  private static final String TEST_EMAIL = "test@example.com";

  private TrustedIntermediaryManagementController controller;
  private AccountRepository accountRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);

    controller =
        new TrustedIntermediaryManagementController(
            instanceOf(TrustedIntermediaryGroupListView.class),
            instanceOf(EditTrustedIntermediaryGroupView.class),
            accountRepository,
            instanceOf(FormFactory.class));
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
                .filter(id -> id == group.id))
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
