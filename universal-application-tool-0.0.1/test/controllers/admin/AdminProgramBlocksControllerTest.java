package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class AdminProgramBlocksControllerTest extends WithPostgresContainer {

  private AdminProgramBlocksController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramBlocksController.class);
  }

  @Test
  public void index_withInvalidProgram_notFound() {
    Result result = controller.index(1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void index_withProgram_redirectsToEdit() {
    Program program = ProgramBuilder.newProgram().build();

    Result result = controller.index(program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.edit(program.id, 1L).url());
  }

  @Test
  public void create_withInvalidProgram_notFound() {
    Result result = controller.create(1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void create_withProgram_addsBlock() {
    Program program = ProgramBuilder.newProgram().build();
    Result result = controller.create(program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.edit(program.id, 2L).url());

    program.refresh();
    assertThat(program.getProgramDefinition().blockDefinitions()).hasSize(2);
  }

  @Test
  public void edit_withInvalidProgram_notFound() {
    Request request = fakeRequest().build();
    Result result = controller.edit(request, 1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withInvalidBlock_notFound() {
    Program program = ProgramBuilder.newProgram().build();
    Request request = fakeRequest().build();
    Result result = controller.edit(request, program.id, 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withProgram_OK() {
    Program program = ProgramBuilder.newProgram().build();
    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.edit(request, program.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_withInvalidProgram_notFound() {
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    Result result = controller.update(request, 1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_withInvalidBlockId_notFound() {
    Program program = ProgramBuilder.newProgram().build();
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    Result result = controller.update(request, program.id, 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_overwritesExistingBlock() {
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "updated name", "description", "udpated description"))
            .build();

    Result result =
        controller.update(request, program.id(), program.getBlockDefinitionByIndex(0).get().id());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(
                    program.id(), program.getBlockDefinitionByIndex(0).get().id())
                .url());

    Result redirectResult =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.id(),
            program.getBlockDefinitionByIndex(0).get().id());
    assertThat(contentAsString(redirectResult)).contains("updated name");
  }

  @Test
  public void destroy_withInvalidProgram_notFound() {
    Result result = controller.destroy(1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void destroy_programWithTwoBlocks_redirects() {
    Program program = ProgramBuilder.newProgram().withBlock().withBlock().build();
    Result result = controller.destroy(program.id, 1L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id).url());
  }

  @Test
  public void destroy_lastBlock_notFound() {
    Program program = ProgramBuilder.newProgram().build();
    Result result = controller.destroy(program.id, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
