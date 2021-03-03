package repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import services.applicant.Path;

public class ApplicantRepositoryTest extends WithPostgresContainer {

  private ApplicantRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(ApplicantRepository.class);
  }

  @Test
  public void listApplicants_empty() {
    Set<Applicant> allApplicants = repo.listApplicants().toCompletableFuture().join();

    assertThat(allApplicants).isEmpty();
  }

  @Test
  public void listApplicants() {
    Applicant one = saveApplicant("one");
    Applicant two = saveApplicant("two");

    Set<Applicant> allApplicants = repo.listApplicants().toCompletableFuture().join();

    assertThat(allApplicants).containsExactly(one, two);
  }

  @Test
  public void lookupApplicant_returnsEmptyOptionalWhenApplicantNotFound() {
    Optional<Applicant> found = repo.lookupApplicant(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupApplicant_findsCorrectApplicant() {
    saveApplicant("Alice");
    Applicant two = saveApplicant("Bob");

    Optional<Applicant> found = repo.lookupApplicant(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void insertApplicant() throws Exception {
    Applicant applicant = new Applicant();
    String path = "$.applicant.birthdate";
    applicant.getApplicantData().put(Path.create(path), "1/1/2021");

    assertThat(applicant.getApplicantData().read(Path.create(path), String.class))
        .hasValue("1/1/2021");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    Applicant a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().read(Path.create(path), String.class)).hasValue("1/1/2021");
  }

  @Test
  public void lookupApplicantSync_returnsEmptyOptionalWhenApplicantNotFound() {
    Optional<Applicant> found = repo.lookupApplicantSync(1L);

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupApplicantSync_findsCorrectApplicant() {
    saveApplicant("Alice");
    Applicant two = saveApplicant("Bob");

    Optional<Applicant> found = repo.lookupApplicantSync(two.id);

    assertThat(found).hasValue(two);
  }

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant.getApplicantData().put(Path.create("$.applicant.name"), name);
    applicant.save();
    return applicant;
  }
}
