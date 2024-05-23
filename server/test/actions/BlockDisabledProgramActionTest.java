package actions;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletionStage;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;
import services.program.ProgramService;

public class BlockDisabledProgramActionTest extends WithApplication {
  @Test
  public void testProgramSlugIsNotAvailable() throws Exception {
    ProgramService programService = instanceOf(ProgramService.class);
    BlockDisabledProgramAction action = new BlockDisabledProgramAction(programService);

    Request request = Helpers.fakeRequest().build();

    CompletionStage<Result> result = action.call(request);
    assertEquals(action.delegate.call(request), result);
  }
}
