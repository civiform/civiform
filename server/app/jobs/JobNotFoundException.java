package jobs;

public class JobNotFoundException extends Exception {

  public JobNotFoundException(String jobName) {
    super("No job found in registry with name: " + jobName);
  }
}
