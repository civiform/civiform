package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class AccountTest extends WithPostgresContainer {

  @Test
  public void canAddAdministeredProgram() {
    Account account = new Account();
    ProgramDefinition one = ProgramBuilder.newDraftProgram("one").build().getProgramDefinition();
    ProgramDefinition two = ProgramBuilder.newDraftProgram("two").build().getProgramDefinition();

    account.addAdministeredProgram(one);
    account.addAdministeredProgram(two);
    assertThat(account.getAdministeredProgramNames()).containsExactly("one", "two");
  }
}
