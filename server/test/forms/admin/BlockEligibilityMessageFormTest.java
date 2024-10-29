package forms.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http;
import repository.ResetPostgres;

public class BlockEligibilityMessageFormTest extends ResetPostgres {

  private FormFactory formFactory;

  @Before
  private void setup() {
    formFactory = instanceOf(FormFactory.class);
  }

  @Test
  public void bindFromRequest_hasEligibilityMessage() {
    String eligibilityMsg = "This is a test eligibility message";
    ImmutableMap<String, String> requestData =
        ImmutableMap.of(BlockEligibilityMessageForm.ELIGIBILITY_MESSAGE, eligibilityMsg);
    Http.Request request = fakeRequestBuilder().bodyForm(requestData).build();

    BlockEligibilityMessageForm form =
        formFactory
            .form(BlockEligibilityMessageForm.class)
            .bindFromRequest(
                request, BlockEligibilityMessageForm.FIELD_NAMES.toArray(new String[0]))
            .get();

    assertThat(form.getEligibilityMessage()).isEqualTo(eligibilityMsg);
  }
}
