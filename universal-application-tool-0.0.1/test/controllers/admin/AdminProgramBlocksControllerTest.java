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
import services.program.BlockDefinition;
import services.program.ProgramDefinition;

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
  public void index_withProgramWithBlocks_redirectsToEdit() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("block").setDescription("desc").build();
    Program program = resourceCreator().insertProgram("program", block);

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
    Program program = resourceCreator().insertProgram("program");
    Result result = controller.create(program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.edit(program.id, 2L).url());

    program.refresh();
    assertThat(program.getProgramDefinition().blockDefinitions()).hasSize(2);
  }

  @Test
  public void create_withProgramWithBlock_addsBlock() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("block").setDescription("desc").build();
    Program program = resourceCreator().insertProgram("program", block);
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
    Request request = fakeRequest().build();
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("block").setDescription("desc").build();
    Program program = resourceCreator().insertProgram("program", block);
    Result result = controller.edit(request, program.id, 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withProgramWithBlock_OK() {
    Request request = addCSRFToken(fakeRequest()).build();
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("block").setDescription("desc").build();
    Program program = resourceCreator().insertProgram("program", block);
    Result result = controller.edit(request, program.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_noProgram_notFound() {
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    Result result = controller.update(request, 1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_overwritesExistingBlock() {
    ProgramDefinition program = resourceCreator().insertProgramWithOneBlock("program");
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "updated name", "description", "udpated description"))
            .build();

    Result result =
        controller.update(request, program.id(), program.getBlockDefinition(0).get().id());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(
                    program.id(), program.getBlockDefinition(0).get().id())
                .url());

    Result redirectResult =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.id(),
            program.getBlockDefinition(0).get().id());
    assertThat(contentAsString(redirectResult)).contains("updated name");
  }

  @Test
  public void destroy_withInvalidProgram_notFound() {
    Result result = controller.destroy(1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void destroy_withProgram_redirects() {
    ProgramDefinition program = resourceCreator().insertProgramWithOneBlock("program");
    controller.create(program.id());
    Result result = controller.destroy(program.id(), 1L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id()).url());
  }

  @Test
  public void destroy_lastBlock_notFound() {
    Program program = resourceCreator().insertProgram("program");
    Result result = controller.destroy(program.id, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
