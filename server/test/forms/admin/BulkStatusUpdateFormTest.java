package forms.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http;
import repository.ResetPostgres;

public class BulkStatusUpdateFormTest extends ResetPostgres {

  private FormFactory formFactory;

  @Before
  public void setup() {
    formFactory = instanceOf(FormFactory.class);
  }

  @Test
  public void bindFromRequest_hasAllFields() {
    ImmutableMap<String, String> requestData =
        ImmutableMap.of(
            "applicationsIds[0]",
            "1",
            "applicationsIds[1]",
            "2",
            "applicationsIds[2]",
            "3",
            "statusText",
            "approved",
            "shouldSendEmail",
            "false");
    Http.Request request = fakeRequestBuilder().bodyForm(requestData).build();

    BulkStatusUpdateForm form =
        formFactory.form(BulkStatusUpdateForm.class).bindFromRequest(request).get();

    assertThat(form.getApplicationsIds().get(0)).isEqualTo("1");
    assertThat(form.getApplicationsIds().get(1)).isEqualTo("2");
    assertThat(form.getApplicationsIds().get(2)).isEqualTo("3");
    assertThat(form.getStatusText()).isEqualTo("approved");
    assertThat(form.sendEmail()).isFalse();
  }
}
