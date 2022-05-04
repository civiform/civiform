package services.apikey;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ApiKeyGrants.Permission;
import auth.CiviFormProfile;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.KeyGenerator;
import models.ApiKey;
import org.apache.commons.net.util.SubnetUtils;
import play.Environment;
import play.data.DynamicForm;
import repository.ApiKeyRepository;
import services.DateConverter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

public class ApiKeyService {

  private static final int KEY_ID_LENGTH = 128;
  private static final int KEY_SECRET_LENGTH = 256;

  private final Environment environment;
  private final ProgramService programService;
  private final ApiKeyRepository repository;
  private final String secretSalt;

  @Inject
  public ApiKeyService(
      ApiKeyRepository repository,
      Environment environment,
      Config config,
      ProgramService programService) {
    this.environment = checkNotNull(environment);
    this.repository = checkNotNull(repository);
    this.programService = checkNotNull(programService);
    this.secretSalt = checkNotNull(config).getString("api_secret_salt");
  }

  public ApiKeyMutationResult createApiKey(DynamicForm form, CiviFormProfile profile)
      throws ProgramNotFoundException {
    if (environment.isProd() && secretSalt.equals("changeme")) {
      throw new RuntimeException("Must set api_secret_salt in production environment.");
    }

    ApiKey apiKey = new ApiKey();

    form = resolveKeyName(form, apiKey);
    form = resolveExpiration(form, apiKey);
    form = resolveSubnet(form, apiKey);
    form = resolveGrants(form, apiKey);

    if (form.hasErrors()) {
      return new ApiKeyMutationResult(apiKey, form);
    }

    String keyId = generateSecret(KEY_ID_LENGTH);
    String keySecret = generateSecret(KEY_SECRET_LENGTH);
    String saltedSecret = salt(keySecret);
    String rawCredentials = keyId + ":" + keySecret;
    String credentials =
        Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

    apiKey.setKeyId(keyId);
    apiKey.setSaltedKeySecret(saltedSecret);
    apiKey.setCreatedBy(getAuthorityId(profile));

    apiKey = repository.insert(apiKey).toCompletableFuture().join();

    return new ApiKeyMutationResult(apiKey, form, credentials);
  }

  private DynamicForm resolveKeyName(DynamicForm form, ApiKey apiKey) {
    Optional<String> maybeKeyName =
        Optional.ofNullable(form.rawData().getOrDefault("keyName", null));

    if (maybeKeyName.isPresent() && !maybeKeyName.get().isBlank()) {
      apiKey.setName(maybeKeyName.get());
    } else {
      form = form.withError("keyName", "Key name cannot be blank.");
    }

    return form;
  }

  private DynamicForm resolveExpiration(DynamicForm form, ApiKey apiKey) {
    Optional<String> maybeExpirationString =
        Optional.ofNullable(form.rawData().getOrDefault("expiration", null));

    if (!maybeExpirationString.isPresent() || maybeExpirationString.get().isBlank()) {
      return form.withError("expiration", "Expiration cannot be blank.");
    }

    try {
      Instant expiration =
          DateConverter.parseIso8601DateToStartOfDateInstant(maybeExpirationString.get());
      apiKey.setExpiration(expiration);
    } catch (DateTimeParseException e) {
      return form.withError("expiration", "Expiration must be in the form YYYY-MM-DD.");
    }

    return form;
  }

  private DynamicForm resolveSubnet(DynamicForm form, ApiKey apiKey) {
    Optional<String> maybeSubnetString =
        Optional.ofNullable(form.rawData().getOrDefault("subnet", null));

    if (!maybeSubnetString.isPresent() || maybeSubnetString.get().isBlank()) {
      return form.withError("subnet", "Subnet cannot be blank.");
    }

    try {
      new SubnetUtils(maybeSubnetString.get());
    } catch (IllegalArgumentException e) {
      return form.withError("subnet", "Subnet must be in CIDR notation.");
    }

    apiKey.setSubnet(maybeSubnetString.get());

    return form;
  }

  private static final Pattern GRANT_PROGRAM_READ_PATTERN =
      Pattern.compile("^grant-program-read\\[([\\w\\-]+)\\]$");

  private DynamicForm resolveGrants(DynamicForm form, ApiKey apiKey)
      throws ProgramNotFoundException {
    ImmutableSet<String> programSlugs = programService.getAllProgramSlugs();

    for (String formDataKey : form.rawData().keySet()) {
      Matcher matcher = GRANT_PROGRAM_READ_PATTERN.matcher(formDataKey);

      if (!matcher.find()) {
        continue;
      }

      matcher.matches();

      String programSlug = matcher.group(1);

      if (!programSlugs.contains(programSlug)) {
        throw new ProgramNotFoundException(programSlug);
      }

      apiKey.getGrants().grantProgramPermission(programSlug, Permission.READ);
    }

    return form;
  }

  private String generateSecret(int length) {
    KeyGenerator keyGen;

    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    keyGen.init(length);

    byte[] secret = keyGen.generateKey().getEncoded();
    return Base64.getEncoder().encodeToString(secret);
  }

  private String salt(String message) {
    byte[] rawMessage = Base64.getDecoder().decode(message);
    byte[] rawKey = Base64.getDecoder().decode(secretSalt);

    HashFunction hashFunction = Hashing.hmacSha256(rawKey);
    HashCode saltedMessage = hashFunction.hashBytes(rawMessage);

    return Base64.getEncoder().encodeToString(saltedMessage.asBytes());
  }

  private String getAuthorityId(CiviFormProfile profile) {
    String authorityId = profile.getAuthorityId().join();

    if (authorityId != null) {
      return authorityId;
    }

    if (environment.isDev()) {
      return "dev-admin";
    }

    throw new RuntimeException("ApiKey creator must have authority_id.");
  }

  public static class ApiKeyMutationResult {
    private final DynamicForm form;
    private final ApiKey apiKey;
    private final Optional<String> credentials;

    public ApiKeyMutationResult(ApiKey apiKey, DynamicForm form) {
      this.apiKey = checkNotNull(apiKey);
      this.form = checkNotNull(form);
      this.credentials = Optional.empty();
    }

    public ApiKeyMutationResult(ApiKey apiKey, DynamicForm form, String credentials) {
      this.apiKey = checkNotNull(apiKey);
      this.form = checkNotNull(form);
      this.credentials = Optional.of(credentials);
    }

    public ApiKey getApiKey() {
      return apiKey;
    }

    public DynamicForm getForm() {
      return form;
    }

    public String getCredentials() {
      return credentials.get();
    }
  }
}
