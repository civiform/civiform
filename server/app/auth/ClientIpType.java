package auth;

/** Where to find the IP address for incoming requests. */
public enum ClientIpType {
  // The IP address of the HTTP client is the originating IP address.
  DIRECT,
  // Incoming requests are reverse proxied and the originating IP address is stored in the
  // X-Forwarded-For header.
  FORWARDED;
}
