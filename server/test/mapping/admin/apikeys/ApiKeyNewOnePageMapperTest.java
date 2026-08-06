package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import services.MessageKey;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.FormField;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.ProgramCheckbox;

public final class ApiKeyNewOnePageMapperTest {

  private static final ImmutableSet<String> PROGRAM_NAMES =
      ImmutableSet.of("Utility Discount", "Housing Voucher");

  private ApiKeyNewOnePageMapper mapper;
  private Messages enUsMessages;

  @Before
  public void setup() {
    mapper = new ApiKeyNewOnePageMapper();

    Langs langs = new Langs(new play.api.i18n.DefaultLangs());
    Map<String, String> messagesMap = new HashMap<>();
    messagesMap.put(MessageKey.TOAST_ERROR_MSG_OUTLINE.getKeyName(), "Error: {0}");
    Map<String, Map<String, String>> langMap =
        Collections.singletonMap(Lang.defaultLang().code(), messagesMap);
    MessagesApi messagesApi = play.test.Helpers.stubMessagesApi(langMap, langs);
    enUsMessages = messagesApi.preferred(langs.availables());
  }

  private ApiKeyNewOnePageViewModel mapWithoutForm() {
    return mapper.map(
        PROGRAM_NAMES, /* maybeForm= */ Optional.empty(), enUsMessages, /* showForm= */ true);
  }

  private ApiKeyNewOnePageViewModel mapWithForm(DynamicForm form) {
    return mapper.map(PROGRAM_NAMES, Optional.of(form), enUsMessages, /* showForm= */ true);
  }

  @Test
  public void map_setsTitle() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getTitle()).isEqualTo("Create a new API key");
  }

  @Test
  public void map_setsFormActionUrl() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getFormActionUrl()).isEqualTo("/admin/apiKeys");
  }

  @Test
  public void map_setsFieldIdsNamesAndLabels() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getKeyNameField().getId()).isEqualTo("keyName");
    assertThat(result.getKeyNameField().getName()).isEqualTo("keyName");
    assertThat(result.getKeyNameField().getLabel()).isEqualTo("API key name");
    assertThat(result.getExpirationField().getId()).isEqualTo("expiration");
    assertThat(result.getExpirationField().getLabel()).isEqualTo("Expiration date");
    assertThat(result.getSubnetField().getId()).isEqualTo("subnet");
    assertThat(result.getSubnetField().getLabel()).isEqualTo("API key subnet");
  }

  @Test
  public void map_setsExpirationFieldToDateType() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getKeyNameField().getType()).isEqualTo("text");
    assertThat(result.getExpirationField().getType()).isEqualTo("date");
    assertThat(result.getSubnetField().getType()).isEqualTo("text");
  }

  @Test
  public void map_setsErrorsIdFromFieldId() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getKeyNameField().getErrorsId()).isEqualTo("keyName-errors");
  }

  @Test
  public void map_noForm_leavesFieldsEmptyAndErrorFree() {
    FormField result = mapWithoutForm().getKeyNameField();

    assertThat(result.getValue()).isEmpty();
    assertThat(result.isHasError()).isFalse();
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void map_withForm_setsSubmittedValue() {
    DynamicForm form = mock(DynamicForm.class);
    doReturn(Optional.of("my key")).when(form).value("keyName");

    FormField result = mapWithForm(form).getKeyNameField();

    assertThat(result.getValue()).isEqualTo("my key");
  }

  @Test
  public void map_withFieldError_setsOutlinedErrorMessage() {
    DynamicForm form = mock(DynamicForm.class);
    doReturn(Optional.of(new ValidationError("subnet", "Subnet cannot be blank.")))
        .when(form)
        .error("subnet");

    FormField result = mapWithForm(form).getSubnetField();

    assertThat(result.isHasError()).isTrue();
    assertThat(result.getErrorMessage()).isEqualTo("Error: Subnet cannot be blank.");
  }

  @Test
  public void map_withProgramsError_setsShowProgramsError() {
    DynamicForm form = mock(DynamicForm.class);
    doReturn(Optional.of(new ValidationError("programs", ""))).when(form).error("programs");

    assertThat(mapWithForm(form).isShowProgramsError()).isTrue();
  }

  @Test
  public void map_noForm_doesNotShowProgramsError() {
    assertThat(mapWithoutForm().isShowProgramsError()).isFalse();
  }

  @Test
  public void map_sortsProgramCheckboxesCaseInsensitively() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getProgramCheckboxes())
        .extracting(ProgramCheckbox::getLabel)
        .containsExactly("Housing Voucher", "Utility Discount");
  }

  @Test
  public void map_setsProgramCheckboxIdAndFieldName() {
    ProgramCheckbox result = mapWithoutForm().getProgramCheckboxes().get(0);

    assertThat(result.getId()).isEqualTo("housing-voucher");
    assertThat(result.getName()).isEqualTo("grant-program-read[housing-voucher]");
    assertThat(result.isChecked()).isFalse();
  }

  @Test
  public void map_withGrantedProgram_checksThatProgram() {
    DynamicForm form = mock(DynamicForm.class);
    doReturn(Optional.of("true")).when(form).value("grant-program-read[housing-voucher]");

    ApiKeyNewOnePageViewModel result = mapWithForm(form);

    assertThat(result.getProgramCheckboxes().get(0).isChecked()).isTrue();
    assertThat(result.getProgramCheckboxes().get(1).isChecked()).isFalse();
  }

  @Test
  public void map_withoutPrograms_hidesForm() {
    ApiKeyNewOnePageViewModel result =
        mapper.map(
            ImmutableSet.of(),
            /* maybeForm= */ Optional.empty(),
            enUsMessages,
            /* showForm= */ false);

    assertThat(result.isShowForm()).isFalse();
    assertThat(result.getProgramCheckboxes()).isEmpty();
  }
}
