package jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import models.PersistedDurableJob;
import org.junit.Test;

public class DurableJobRegistryTest {

  @Test
  public void registeringAndAccessingAJob() throws Exception {
    var registry = new DurableJobRegistry();

    registry.register(DurableJobName.OLD_JOB_CLEANUP, new FakeJobFactory());

    assertThat(registry.get(DurableJobName.OLD_JOB_CLEANUP))
        .isInstanceOf(DurableJobRegistry.RegisteredJob.class);
    assertThat(registry.getRecurringJobs()).isEmpty();
  }

  @Test
  public void accessingAMissingJob_throwsException() {
    var registry = new DurableJobRegistry();

    assertThrows(JobNotFoundException.class, () -> registry.get(DurableJobName.OLD_JOB_CLEANUP));
  }

  @Test
  public void registerAndGetRecurringJobs() {
    var registry = new DurableJobRegistry();

    registry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        new FakeJobFactory(),
        new RecurringJobSchedulers.EverySunday2Am());

    assertThat(registry.getRecurringJobs().size()).isEqualTo(1);
  }

  static class FakeJobFactory implements DurableJobFactory {

    @Override
    public DurableJob create(PersistedDurableJob persistedDurableJob) {
      return new DurableJob() {
        @Override
        public PersistedDurableJob getPersistedDurableJob() {
          return persistedDurableJob;
        }

        @Override
        public void run() {}
      };
    }
  }
}
