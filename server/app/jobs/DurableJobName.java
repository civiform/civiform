package jobs;

public enum DurableJobName {
  OLD_JOB_CLEANUP("OLD_JOB_CLEANUP");

  private final String jobName;

  DurableJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getJobName() {
    return jobName;
  }
}
