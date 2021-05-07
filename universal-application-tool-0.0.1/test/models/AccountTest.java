package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import repository.WithPostgresContainer;
import support.ProgramBuilder;

public class AccountTest extends WithPostgresContainer {

  @Test
  public void canAddAdministeredProgram() {
    Account account = new Account();
    Program one = ProgramBuilder.newDraftProgram().build();
    Program two = ProgramBuilder.newDraftProgram().build();

    account.setAdministeredPrograms(ImmutableList.of(one));
    assertThat(account.getAdministeredPrograms()).containsExactly(one);

    account.addAdministeredProgram(two);
    assertThat(account.getAdministeredPrograms()).containsExactly(one, two);
  }
}
