package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import models.PersistedDurableJobModel;
import org.junit.Test;

public class DurableJobRegistryTest {

  @Test
  public void get_aMissingJob_throwsException() {
    var registry = new DurableJobRegistry();

    assertThatThrownBy(() -> registry.get(DurableJobName.OLD_JOB_CLEANUP))
        .isInstanceOf(JobNotFoundException.class);
  }

  @Test
  public void register_andGetRecurringJobs() {
    var registry = new DurableJobRegistry();

    assertThat(registry.getRecurringJobs().size()).isEqualTo(0);

    registry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        new FakeJobFactory(),
        new ExecutionTimeResolvers.Sunday2Am());

    assertThat(registry.getRecurringJobs().size()).isEqualTo(1);
  }

  @Test
  public void register_withADuplicateName_throwsAnException() {
    var registry = new DurableJobRegistry();

    registry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        new FakeJobFactory(),
        new ExecutionTimeResolvers.Sunday2Am());

    assertThatThrownBy(
            () ->
                registry.register(
                    DurableJobName.OLD_JOB_CLEANUP,
                    new FakeJobFactory(),
                    new ExecutionTimeResolvers.Sunday2Am()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static class FakeJobFactory implements DurableJobFactory {

    @Override
    public DurableJob create(PersistedDurableJobModel persistedDurableJob) {
      return new DurableJob() {
        @Override
        public PersistedDurableJobModel getPersistedDurableJob() {
          return persistedDurableJob;
        }

        @Override
        public void run() {}
      };
    }
  }
}
