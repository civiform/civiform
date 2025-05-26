package views.admin.apibridge.discovery;

public final class DiscoveryViewModel {
  public String hostUri;

  public DiscoveryViewModel() {}

  public DiscoveryViewModel(String hostUri) {
    this.hostUri = hostUri;
  }

  public String hostUri() {
    return hostUri;
  }

  public DiscoveryViewModel setHostUri(String hostUri) {
    this.hostUri = hostUri;
    return this;
  }
}
