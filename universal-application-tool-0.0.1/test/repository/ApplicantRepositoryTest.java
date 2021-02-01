package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import models.Applicant;
import org.junit.Test;

public class ApplicantRepositoryTest extends WithPostgresContainer {

  @Test
  public void createApplicant() {
    // arrange
    final ApplicantRepository repo = app.injector().instanceOf(ApplicantRepository.class);
    Applicant applicant = new Applicant();
    applicant.id = 1L;
    applicant.setObject(
        ImmutableMap.of("nestedObject", ImmutableMap.of("foo", "bar"), "secondKey", "value"));
    // act
    repo.insertApplicant(applicant).toCompletableFuture().join();
    // assert
    Applicant a = repo.lookupApplicant(1L).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(1L);
    assertThat(a.getObject()).containsAllEntriesOf(applicant.getObject());
  }
}
