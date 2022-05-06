package services.apikey;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ApiKeyGrants;
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

/**
 * Service management of the resource backed by {@link ApiKey}.
 *
 * <p>CiviForm uses API keys to implement HTTP basic auth
 * (https://en.wikipedia.org/wiki/Basic_access_authentication) for authenticating API requests, as
 * well as for authorizing requests based on the permission grants stored with the key in the
 * database.
 *
 * <p>For a given key, CiviForm stores the key secret in plain text along with the key secret hashed
 * with a secret salt value provided to the server as a configuration variable.
 *
 * <p>The plaintext secret is revealed to the admin user once after creation, after which it is not
 * recoverable from CiviForm.
 */
public class ApiKeyService {

  public static final String FORM_FIELD_NAME_KEY_NAME = "keyName";
  public static final String FORM_FIELD_NAME_EXPIRATION = "expiration";
  public static final String FORM_FIELD_NAME_SUBNET = "subnet";

  private static final int KEY_ID_LENGTH = 128;
  private static final int KEY_SECRET_LENGTH = 256;

  private final Environment environment;
  private final ProgramService programService;
  private final ApiKeyRepository repository;
  private final String secretSalt;
  private final boolean banGlobalSubnet;

  @Inject
  public ApiKeyService(
      ApiKeyRepository repository,
      Environment environment,
      ProgramService programService,
      Config config) {
    this.repository = checkNotNull(repository);
    this.environment = checkNotNull(environment);
    this.programService = checkNotNull(programService);
    this.secretSalt = checkNotNull(config).getString("api_secret_salt");
    this.banGlobalSubnet = checkNotNull(config).getBoolean("api_keys_ban_global_subnet");
  }

  /**
   * Creates a new {@link ApiKey} with the data in {@code form} if it passes validation.
   *
   * @param form Stores data specified by the user for creation of the key, as well as validation
   *     errors
   * @param profile The user profile of the account creating this API key, used for recording who
   *     created it
   * @return a result object containing the credentials and created key if successful, form with
   *     validation messages if not
   */
  public ApiKeyCreationResult createApiKey(DynamicForm form, CiviFormProfile profile)
      throws ProgramNotFoundException {
    if (environment.isProd() && secretSalt.equals("changeme")) {
      throw new RuntimeException("Must set api_secret_salt in production environment.");
    }

    ApiKeyGrants grants = resolveGrants(form);
    ApiKey apiKey = new ApiKey(grants);

    // apiKey is an ebean entity/model and is mutable. form is play form object and is immutable.
    // adding validation error messages using form.withError returns a new form object with the
    // error message added.
    form = resolveKeyName(form, apiKey);
    form = resolveExpiration(form, apiKey);
    form = resolveSubnet(form, apiKey);

    if (form.hasErrors()) {
      return ApiKeyCreationResult.failure(form);
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

    return ApiKeyCreationResult.success(apiKey, credentials);
  }

  // apiKey is mutable and modified here, form is immutable so a new instance is returned
  private DynamicForm resolveKeyName(DynamicForm form, ApiKey apiKey) {
    String keyName = form.rawData().getOrDefault(FORM_FIELD_NAME_KEY_NAME, "");

    if (!keyName.isBlank()) {
      apiKey.setName(keyName);
    } else {
      form = form.withError(FORM_FIELD_NAME_KEY_NAME, "Key name cannot be blank.");
    }

    return form;
  }

  // apiKey is mutable and modified here, form is immutable so a new instance is returned
  private DynamicForm resolveExpiration(DynamicForm form, ApiKey apiKey) {
    String expirationString = form.rawData().getOrDefault(FORM_FIELD_NAME_EXPIRATION, "");

    if (expirationString.isBlank()) {
      return form.withError(FORM_FIELD_NAME_EXPIRATION, "Expiration cannot be blank.");
    }

    try {
      Instant expiration = DateConverter.parseIso8601DateToStartOfDateInstant(expirationString);
      apiKey.setExpiration(expiration);
    } catch (DateTimeParseException e) {
      return form.withError(
          FORM_FIELD_NAME_EXPIRATION, "Expiration must be in the form YYYY-MM-DD.");
    }

    return form;
  }

  // apiKey is mutable and modified here, form is immutable so a new instance is returned
  private DynamicForm resolveSubnet(DynamicForm form, ApiKey apiKey) {
    String subnetString = form.rawData().getOrDefault(FORM_FIELD_NAME_SUBNET, "");

    if (subnetString.isBlank()) {
      return form.withError(FORM_FIELD_NAME_SUBNET, "Subnet cannot be blank.");
    }

    if (subnetString.endsWith("0") && banGlobalSubnet) {
      return form.withError(FORM_FIELD_NAME_SUBNET, "Subnet cannot allow all IP addresses.");
    }

    try {
      new SubnetUtils(subnetString);
    } catch (IllegalArgumentException e) {
      return form.withError(FORM_FIELD_NAME_SUBNET, "Subnet must be in CIDR notation.");
    }

    apiKey.setSubnet(subnetString);

    return form;
  }

  // Pattern for matching and extracting form field names that specify
  // granting read permission for a program.
  // These field names have the format "grant-program-read[program-slug]"
  // Where "program-slug" is the sluggified name of the program the key should
  // be granted read access for e.g. "grant-program-read[utility-discount-program]".
  private static final Pattern GRANT_PROGRAM_READ_PATTERN =
      Pattern.compile("^grant-program-read\\[([\\w\\-]+)\\]$");

  private ApiKeyGrants resolveGrants(DynamicForm form) throws ProgramNotFoundException {
    ApiKeyGrants grants = new ApiKeyGrants();
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

      grants.grantProgramPermission(programSlug, Permission.READ);
    }

    return grants;
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

  /** Holds state relevant to the result of attempting to create an {@link ApiKey}. */
  public static class ApiKeyCreationResult {
    private final Optional<ApiKey> apiKey;
    private final Optional<String> credentials;
    private final Optional<DynamicForm> form;

    /** Constructs an instance in the case of success. */
    public static ApiKeyCreationResult success(ApiKey apiKey, String credentials) {
      return new ApiKeyCreationResult(
          Optional.of(apiKey), Optional.of(credentials), /* form= */ Optional.empty());
    }

    /** Constructs an instance in the case of failure. */
    public static ApiKeyCreationResult failure(DynamicForm form) {
      return new ApiKeyCreationResult(
          /* apiKey= */ Optional.empty(), /* credentials= */ Optional.empty(), Optional.of(form));
    }

    private ApiKeyCreationResult(
        Optional<ApiKey> apiKey, Optional<String> credentials, Optional<DynamicForm> form) {
      this.apiKey = apiKey;
      this.credentials = credentials;
      this.form = form;
    }

    /** Returns true if the key was created. */
    public boolean successful() {
      return credentials.isPresent();
    }

    /** Returns the API key if successful, throws {@code NullPointerException} if not. */
    public ApiKey getApiKey() {
      return apiKey.get();
    }

    /**
     * Returns the form with validation errors if failure, throws {@code NullPointerException} if
     * not.
     */
    public DynamicForm getForm() {
      return form.get();
    }

    /**
     * Returns the base64 encoded credentials string if successful, throws {@code
     * NullPointerException} if not.
     */
    public String getCredentials() {
      return credentials.get();
    }
  }
}
