package forms.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http;
import repository.ResetPostgres;

public class ProgramImageDescriptionFormTest extends ResetPostgres {

  private FormFactory formFactory;

  @Before
  public void setup() {
    formFactory = instanceOf(FormFactory.class);
  }

  @Test
  public void bindFromRequest_hasDescription() {
    ImmutableMap<String, String> requestData =
        ImmutableMap.of(ProgramImageDescriptionForm.SUMMARY_IMAGE_DESCRIPTION, "fake description");
    Http.Request request = fakeRequestBuilder().bodyForm(requestData).build();

    ProgramImageDescriptionForm form =
        formFactory
            .form(ProgramImageDescriptionForm.class)
            .bindFromRequest(
                request, ProgramImageDescriptionForm.FIELD_NAMES.toArray(new String[0]))
            .get();

    assertThat(form.getSummaryImageDescription()).isEqualTo("fake description");
  }
}
