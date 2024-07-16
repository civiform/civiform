package views.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.inject.NewInstanceInjector.instanceOf;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http;
import repository.ResetPostgres;

public class AdminProgramExportFormTest extends ResetPostgres {
  private FormFactory formFactory;

  @Before
  public void setup() {
    formFactory = instanceOf(FormFactory.class);
  }

  @Test
  public void bindFromRequest_hasProgramId() {
    ImmutableMap<String, String> requestData =
        ImmutableMap.of(AdminProgramExportForm.PROGRAM_ID_FIELD, "78");
    Http.Request request = fakeRequestBuilder().bodyForm(requestData).build();

    AdminProgramExportForm form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]))
            .get();

    assertThat(form.getProgramId()).isEqualTo(78);
  }
}
