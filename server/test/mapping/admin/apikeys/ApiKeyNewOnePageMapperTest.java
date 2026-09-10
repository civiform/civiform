package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.ProgramCheckbox;

public final class ApiKeyNewOnePageMapperTest {

  private static final ImmutableSet<String> PROGRAM_NAMES =
      ImmutableSet.of("Utility Discount", "Housing Voucher");

  private ApiKeyNewOnePageMapper mapper;

  @Before
  public void setup() {
    mapper = new ApiKeyNewOnePageMapper();
  }

  private ApiKeyNewOnePageViewModel mapWithoutForm() {
    return mapper.map(PROGRAM_NAMES, /* maybeForm= */ Optional.empty());
  }

  private ApiKeyNewOnePageViewModel mapWithForm(DynamicForm form) {
    return mapper.map(PROGRAM_NAMES, Optional.of(form));
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
  public void map_noForm_leavesFieldsEmptyAndErrorFree() {
    ApiKeyNewOnePageViewModel result = mapWithoutForm();

    assertThat(result.getKeyNameValue()).isEmpty();
    assertThat(result.getExpirationValue()).isEmpty();
    assertThat(result.getSubnetValue()).isEmpty();
    assertThat(result.getKeyNameError()).isNull();
    assertThat(result.getExpirationError()).isNull();
    assertThat(result.getSubnetError()).isNull();
  }

  @Test
  public void map_withForm_setsSubmittedValues() {
    DynamicForm form = mock(DynamicForm.class);
    doReturn(Optional.of("my key")).when(form).value("keyName");
    doReturn(Optional.of("2030-01-01")).when(form).value("expiration");

    ApiKeyNewOnePageViewModel result = mapWithForm(form);

    assertThat(result.getKeyNameValue()).isEqualTo("my key");
    assertThat(result.getExpirationValue()).isEqualTo("2030-01-01");
    assertThat(result.getSubnetValue()).isEmpty();
  }

  @Test
  public void map_withFieldError_setsOutlinedErrorMessage() {
    DynamicForm form = mock(DynamicForm.class);
    doReturn(Optional.of(new ValidationError("subnet", "Subnet cannot be blank.")))
        .when(form)
        .error("subnet");

    ApiKeyNewOnePageViewModel result = mapWithForm(form);

    assertThat(result.getSubnetError()).isEqualTo("Error: Subnet cannot be blank.");
    assertThat(result.getKeyNameError()).isNull();
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
        mapper.map(ImmutableSet.of(), /* maybeForm= */ Optional.empty());

    assertThat(result.isShowForm()).isFalse();
    assertThat(result.getProgramCheckboxes()).isEmpty();
  }

  @Test
  public void map_withFormAndWithoutPrograms_showsForm() {
    // A rejected submission re-renders the form even with no program left to grant
    // access to, as the legacy view did.
    ApiKeyNewOnePageViewModel result =
        mapper.map(ImmutableSet.of(), Optional.of(mock(DynamicForm.class)));

    assertThat(result.isShowForm()).isTrue();
  }
}
