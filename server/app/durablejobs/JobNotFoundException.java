package durablejobs;

/** Thrown when no {@link DurableJob} is found for a given {@link DurableJobName}. */
public class JobNotFoundException extends Exception {

  public JobNotFoundException(String jobName) {
    super("No job found in registry with name: " + jobName);
  }
}
