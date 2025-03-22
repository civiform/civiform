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
    accountRepository.createNewTrustedIntermediaryGroup("Group 1", "Description 1");
    accountRepository.createNewTrustedIntermediaryGroup("Group 2", "Description 2");

    var result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Group 1");
    assertThat(contentAsString(result)).contains("Group 2");
  }

  @Test
  public void create_withValidData_createsNewTiGroup() {
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("name", "New Group", "description", "New Description"))
            .build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(accountRepository.listTrustedIntermediaryGroups()).hasSize(1);
    assertThat(accountRepository.listTrustedIntermediaryGroups().get(0).getName())
        .isEqualTo("New Group");
  }

  @Test
  public void create_withoutName_showsErrorMessage() {
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("description", "New Description")).build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).contains("Must provide group name.");
  }

  @Test
  public void create_withoutDescription_showsErrorMessage() {
    var request = fakeRequestBuilder().bodyForm(ImmutableMap.of("name", "New Group")).build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).contains("Must provide group description.");
  }

  @Test
  public void create_withoutUniqueName_showsErrorMessage() {
    accountRepository.createNewTrustedIntermediaryGroup("Duplicate Group", "Description");
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("name", "Duplicate Group", "description", "New Description"))
            .build();

    var result = controller.create(request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).contains("Must provide a unique group name.");
  }

  @Test
  public void edit_withValidId_displaysGroupDetails() {
    var group = accountRepository.createNewTrustedIntermediaryGroup("Group 1", "Description 1");
    accountRepository.addTrustedIntermediaryToGroup(group.id, "test@example.com");

    var result = controller.edit(group.id, fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    // TODO(#10097): Assert that the title of the group is shown
    assertThat(contentAsString(result)).contains("test@example.com");
  }

  @Test
  public void edit_withInvalidId_returnsNotFound() {
    var result = controller.edit(999L, fakeRequest());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("no such group.");
  }

  @Test
  public void delete_withValidId_deletesGroup() {
    var group = accountRepository.createNewTrustedIntermediaryGroup("Group 1", "Description 1");

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
    var group = accountRepository.createNewTrustedIntermediaryGroup("Group 1", "Description 1");
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("emailAddress", "test@example.com")).build();

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
        .contains("test@example.com");
  }

  @Test
  public void addIntermediary_withInvalidGroupId_showsErrorMessage() {
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("emailAddress", "test@example.com")).build();

    var result = controller.addIntermediary(999L, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).contains("No such TI group.");
  }

  @Test
  public void removeIntermediary_withValidData_removesIntermediary() {
    var group = accountRepository.createNewTrustedIntermediaryGroup("Group 1", "Description 1");
    accountRepository.addTrustedIntermediaryToGroup(group.id, "test@example.com");
    var ti =
        accountRepository
            .getTrustedIntermediaryGroup(group.id)
            .get()
            .getTrustedIntermediaries()
            .get(0);
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("accountId", ti.id.toString())).build();

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
        .doesNotContain("test@example.com");
  }

  @Test
  public void removeIntermediary_withInvalidGroupId_showsErrorMessage() {
    AccountModel ti = new AccountModel();
    ti.setEmailAddress("test@example.com");
    ti.save();
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("accountId", ti.id.toString())).build();

    var result = controller.removeIntermediary(999L, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).contains("No such TI group.");
  }
}
