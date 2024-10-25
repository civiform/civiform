package forms.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
    List<Long> idList = new ArrayList<>();
    idList.add(1L);
    idList.add(2L);
    idList.add(3L);
    var stringIdList = idList.stream().map(e -> String.valueOf(e)).collect(Collectors.toList());

    ImmutableMap<String, String> requestData =
        ImmutableMap.of(
            "applicationsIds[0]",
            stringIdList.get(0),
            "applicationsIds[1]",
            stringIdList.get(1),
            "applicationsIds[2]",
            stringIdList.get(2),
            "statusText",
            "approved",
            "shouldSendEmail",
            "false");
    Http.Request request = fakeRequestBuilder().bodyForm(requestData).build();

    BulkStatusUpdateForm form =
        formFactory.form(BulkStatusUpdateForm.class).bindFromRequest(request).get();

    assertThat(form.getApplicationsIds().get(0)).isEqualTo(1L);
    assertThat(form.getApplicationsIds().get(1)).isEqualTo(2L);
    assertThat(form.getApplicationsIds().get(2)).isEqualTo(3L);
    assertThat(form.getStatusText()).isEqualTo("approved");
    assertThat(form.getShouldSendEmail()).isFalse();
  }
}
