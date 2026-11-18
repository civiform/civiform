package mapping.trustedintermediary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.ti.routes;
import forms.TiClientInfoForm;
import java.time.Clock;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.i18n.Lang;
import play.i18n.Messages;
import services.DateConverter;
import views.trustedintermediary.TrustedIntermediaryEditClientPageViewModel;

public final class TrustedIntermediaryEditClientPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";
  private static final long ACCOUNT_ID = 7L;

  private TrustedIntermediaryEditClientPageMapper mapper;
  private TrustedIntermediaryGroupModel tiGroup;
  private AccountModel account;
  private DateConverter dateConverter;
  private Messages messages;

  @Before
  public void setup() {
    mapper = new TrustedIntermediaryEditClientPageMapper();
    dateConverter = new DateConverter(Clock.systemUTC());
    messages = stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

    tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Food Bank");

    ApplicantModel applicant = mock(ApplicantModel.class);
    when(applicant.getFirstName()).thenReturn(Optional.of("Ada"));

    account = mock(AccountModel.class);
    account.id = ACCOUNT_ID;
    when(account.representativeApplicant()).thenReturn(Optional.of(applicant));
    when(account.getTiNote()).thenReturn("");
  }

  /** A submitted form for Grace, as Play would bind it. */
  private static Form<TiClientInfoForm> submittedForm(boolean hasErrors) {
    TiClientInfoForm data = new TiClientInfoForm();
    data.setFirstName("Grace");

    @SuppressWarnings("unchecked")
    Form<TiClientInfoForm> form = mock(Form.class);
    when(form.value()).thenReturn(Optional.of(data));
    when(form.hasErrors()).thenReturn(hasErrors);
    when(form.errors(anyString())).thenReturn(ImmutableList.of());

    return form;
  }

  private TrustedIntermediaryEditClientPageViewModel map(
      Optional<Form<TiClientInfoForm>> submittedForm) {
    return mapper.map(
        tiGroup,
        account,
        submittedForm,
        /* showNameSuffix= */ false,
        BASE_URL,
        dateConverter,
        messages);
  }

  @Test
  public void map_setsTiGroupName() {
    assertThat(map(/* submittedForm= */ Optional.empty()).getTiGroupName()).isEqualTo("Food Bank");
  }

  @Test
  public void map_setsClientListUrlWithBaseUrlAndFirstPage() {
    assertThat(map(/* submittedForm= */ Optional.empty()).getClientListUrl())
        .isEqualTo(
            BASE_URL
                + routes.TrustedIntermediaryController.dashboard(
                        /* nameQuery= */ Optional.empty(),
                        /* dayQuery= */ Optional.empty(),
                        /* monthQuery= */ Optional.empty(),
                        /* yearQuery= */ Optional.empty(),
                        /* page= */ Optional.of(1))
                    .url());
  }

  @Test
  public void map_setsAccountSettingsUrlWithBaseUrl() {
    assertThat(map(/* submittedForm= */ Optional.empty()).getAccountSettingsUrl())
        .isEqualTo(BASE_URL + routes.TrustedIntermediaryController.accountSettings().url());
  }

  @Test
  public void map_setsEditFormIdAndActionUrlForAccount() {
    TrustedIntermediaryEditClientPageViewModel model = map(/* submittedForm= */ Optional.empty());

    assertThat(model.getForm().getFormId()).isEqualTo("edit-ti");
    assertThat(model.getForm().getActionUrl())
        .isEqualTo(routes.TrustedIntermediaryController.editClient(ACCOUNT_ID).url());
  }

  @Test
  public void map_onFirstShow_fillsFormFromAccount() {
    TrustedIntermediaryEditClientPageViewModel model = map(/* submittedForm= */ Optional.empty());

    assertThat(model.getForm().getFirstName().value()).isEqualTo("Ada");
  }

  @Test
  public void map_onFirstShow_hidesSuccessAlert() {
    assertThat(map(/* submittedForm= */ Optional.empty()).isShowSuccessAlert()).isFalse();
  }

  @Test
  public void map_withFormErrors_hidesSuccessAlert() {
    assertThat(map(Optional.of(submittedForm(/* hasErrors= */ true))).isShowSuccessAlert())
        .isFalse();
  }

  @Test
  public void map_afterSave_showsSuccessAlert() {
    assertThat(map(Optional.of(submittedForm(/* hasErrors= */ false))).isShowSuccessAlert())
        .isTrue();
  }

  @Test
  public void map_afterSubmit_fillsFormFromSubmittedValues() {
    TrustedIntermediaryEditClientPageViewModel model =
        map(Optional.of(submittedForm(/* hasErrors= */ false)));

    assertThat(model.getForm().getFirstName().value()).isEqualTo("Grace");
  }
}
