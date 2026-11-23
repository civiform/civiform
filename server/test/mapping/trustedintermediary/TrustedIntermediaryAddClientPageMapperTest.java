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
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.i18n.Lang;
import play.i18n.Messages;
import services.DateConverter;
import views.trustedintermediary.TrustedIntermediaryAddClientPageViewModel;

public final class TrustedIntermediaryAddClientPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";
  private static final long TI_GROUP_ID = 5L;
  private static final long NEW_CLIENT_APPLICANT_ID = 42L;

  private TrustedIntermediaryAddClientPageMapper mapper;
  private TrustedIntermediaryGroupModel tiGroup;
  private DateConverter dateConverter;
  private Messages messages;

  @Before
  public void setup() {
    mapper = new TrustedIntermediaryAddClientPageMapper();
    dateConverter = new DateConverter(Clock.systemUTC());
    messages = stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

    tiGroup = mock(TrustedIntermediaryGroupModel.class);
    tiGroup.id = TI_GROUP_ID;
    when(tiGroup.getName()).thenReturn("Food Bank");
  }

  /** A submitted form for Ada Lovelace, as Play would bind it. */
  private static Form<TiClientInfoForm> submittedForm(boolean hasErrors) {
    TiClientInfoForm data = new TiClientInfoForm();
    data.setFirstName("Ada");
    data.setLastName("Lovelace");

    @SuppressWarnings("unchecked")
    Form<TiClientInfoForm> form = mock(Form.class);
    when(form.value()).thenReturn(Optional.of(data));
    when(form.hasErrors()).thenReturn(hasErrors);
    when(form.errors(anyString())).thenReturn(ImmutableList.of());

    return form;
  }

  private TrustedIntermediaryAddClientPageViewModel map(
      Optional<Form<TiClientInfoForm>> submittedForm, Optional<Long> newClientApplicantId) {
    return mapper.map(
        tiGroup,
        submittedForm,
        newClientApplicantId,
        /* showNameSuffix= */ false,
        BASE_URL,
        dateConverter,
        messages);
  }

  private TrustedIntermediaryAddClientPageViewModel mapFirstShow() {
    return map(/* submittedForm= */ Optional.empty(), /* newClientApplicantId= */ Optional.empty());
  }

  private TrustedIntermediaryAddClientPageViewModel mapSuccessfulSave() {
    return map(
        Optional.of(submittedForm(/* hasErrors= */ false)), Optional.of(NEW_CLIENT_APPLICANT_ID));
  }

  @Test
  public void map_setsTiGroupName() {
    assertThat(mapFirstShow().getTiGroupName()).isEqualTo("Food Bank");
  }

  @Test
  public void map_setsClientListUrlWithBaseUrlAndFirstPage() {
    assertThat(mapFirstShow().getClientListUrl())
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
    assertThat(mapFirstShow().getAccountSettingsUrl())
        .isEqualTo(BASE_URL + routes.TrustedIntermediaryController.accountSettings().url());
  }

  @Test
  public void map_setsAddFormIdAndActionUrl() {
    TrustedIntermediaryAddClientPageViewModel model = mapFirstShow();

    assertThat(model.getForm().getFormId()).isEqualTo("add-ti");
    assertThat(model.getForm().getActionUrl())
        .isEqualTo(routes.TrustedIntermediaryController.addClient(TI_GROUP_ID).url());
  }

  @Test
  public void map_setsShowNameSuffixOnForm() {
    TrustedIntermediaryAddClientPageViewModel model =
        mapper.map(
            tiGroup,
            /* submittedForm= */ Optional.empty(),
            /* newClientApplicantId= */ Optional.empty(),
            /* showNameSuffix= */ true,
            BASE_URL,
            dateConverter,
            messages);

    assertThat(model.getForm().isShowNameSuffix()).isTrue();
  }

  @Test
  public void map_onFirstShow_leavesFormEmpty() {
    assertThat(mapFirstShow().getForm().getFirstName().value()).isEmpty();
  }

  @Test
  public void map_onFirstShow_hidesSuccessAlertAndStartApplicationButton() {
    TrustedIntermediaryAddClientPageViewModel model = mapFirstShow();

    assertThat(model.isShowSuccessAlert()).isFalse();
    assertThat(model.getNewClientName()).isNull();
    assertThat(model.getStartApplicationUrl()).isNull();
  }

  @Test
  public void map_withFormErrors_hidesSuccessAlertAndStartApplicationButton() {
    TrustedIntermediaryAddClientPageViewModel model =
        map(
            Optional.of(submittedForm(/* hasErrors= */ true)),
            /* newClientApplicantId= */ Optional.empty());

    assertThat(model.isShowSuccessAlert()).isFalse();
    assertThat(model.getStartApplicationUrl()).isNull();
  }

  @Test
  public void map_withFormErrors_keepsSubmittedValues() {
    TrustedIntermediaryAddClientPageViewModel model =
        map(
            Optional.of(submittedForm(/* hasErrors= */ true)),
            /* newClientApplicantId= */ Optional.empty());

    assertThat(model.getForm().getFirstName().value()).isEqualTo("Ada");
  }

  @Test
  public void map_afterSave_showsSuccessAlertNamingTheClient() {
    TrustedIntermediaryAddClientPageViewModel model = mapSuccessfulSave();

    assertThat(model.isShowSuccessAlert()).isTrue();
    assertThat(model.getNewClientName()).isEqualTo("Ada Lovelace");
  }

  @Test
  public void map_afterSave_setsStartApplicationUrlForNewApplicant() {
    assertThat(mapSuccessfulSave().getStartApplicationUrl())
        .isEqualTo(
            controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
                    NEW_CLIENT_APPLICANT_ID, /* categories= */ ImmutableList.of())
                .url());
  }

  @Test
  public void map_afterSaveWithoutApplicantId_leavesStartApplicationUrlNull() {
    TrustedIntermediaryAddClientPageViewModel model =
        map(
            Optional.of(submittedForm(/* hasErrors= */ false)),
            /* newClientApplicantId= */ Optional.empty());

    assertThat(model.isShowSuccessAlert()).isTrue();
    assertThat(model.getStartApplicationUrl()).isNull();
  }
}
