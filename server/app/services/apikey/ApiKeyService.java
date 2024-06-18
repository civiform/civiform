package services.apikey;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ApiKeyGrants;
import auth.ApiKeyGrants.Permission;
import auth.CiviFormProfile;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.NotChangeableException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.KeyGenerator;
import models.ApiKeyModel;
import org.apache.commons.net.util.SubnetUtils;
import play.Environment;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.data.DynamicForm;
import repository.ApiKeyRepository;
import services.CryptographicUtils;
import services.DateConverter;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/**
 * Service management of the resource backed by {@link ApiKeyModel}.
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
public final class ApiKeyService {

  // The cache expiration time is intended to be long enough reduce database queries from
  // authenticating API calls while being short enough that if an admin retires a key or
  // otherwise edits it, the edits will take effect within a reasonable amount of time.
  // When tuning this value, consider the use-case of an API consumer rapidly paging through
  // a list API, and also consider the admin retiring a key when they've discovered it has
  // been compromised.
  private static final int CACHE_EXPIRATION_TIME_SECONDS = 60;

  public static final String FORM_FIELD_NAME_KEY_NAME = "keyName";
  public static final String FORM_FIELD_NAME_EXPIRATION = "expiration";
  public static final String FORM_FIELD_NAME_SUBNET = "subnet";
  public static final String PROGRAMS_FIELD_GROUP_NAME = "programs";

  // This matches the default value specified in application.conf
  // A hard-coded matching value is provided here to ensure that the admin
  // does not create API keys with the default. The check for that is done
  // here rather than requiring the value at runtime to avoid a deployment
  // failing due to not specifying the value in prod.
  public static final String DEFAULT_API_KEY_SECRET_SALT = "changeme";

  private static final int KEY_ID_LENGTH = 128;
  private static final int KEY_SECRET_LENGTH = 256;

  // This is the subnet mask that would represent the full network address space associated
  // with the IP address. In CIDR notation this would be the ending '0' if given '10.1.1.1/0'
  private static final String GLOBAL_SUBNET_NETMASK = "0.0.0.0";

  private final ApiKeyRepository repository;
  private final Environment environment;
  private final ProgramService programService;
  private final DateConverter dateConverter;
  private final String secretSalt;
  private final SyncCacheApi apiKeyCache;
  private final boolean banGlobalSubnet;

  @Inject
  public ApiKeyService(
      @NamedCache("api-keys") SyncCacheApi apiKeyCache,
      ApiKeyRepository repository,
      Environment environment,
      ProgramService programService,
      DateConverter dateConverter,
      Config config) {
    this.apiKeyCache = checkNotNull(apiKeyCache);
    this.repository = checkNotNull(repository);
    this.environment = checkNotNull(environment);
    this.programService = checkNotNull(programService);
    this.dateConverter = checkNotNull(dateConverter);
    this.secretSalt = checkNotNull(config).getString("api_secret_salt");
    this.banGlobalSubnet = checkNotNull(config).getBoolean("api_keys_ban_global_subnet");
  }

  /**
   * Lists all active, i.e. unexpired and unretired, {@link ApiKeyModel}s in order of creation time
   * descending.
   */
  public ImmutableList<ApiKeyModel> listActiveApiKeys() {
    PaginationResult<ApiKeyModel> apiKeys =
        repository.listActiveApiKeys(PageNumberBasedPaginationSpec.MAX_PAGE_SIZE_SPEC);
    return apiKeys.getPageContents();
  }

  /** Lists all retired {@link ApiKeyModel}s in order of creation time descending. */
  public ImmutableList<ApiKeyModel> listRetiredApiKeys() {
    PaginationResult<ApiKeyModel> apiKeys =
        repository.listRetiredApiKeys(PageNumberBasedPaginationSpec.MAX_PAGE_SIZE_SPEC);
    return apiKeys.getPageContents();
  }

  /** Lists all expired {@link ApiKeyModel}s in order of creation time descending. */
  public ImmutableList<ApiKeyModel> listExpiredApiKeys() {
    PaginationResult<ApiKeyModel> apiKeys =
        repository.listExpiredApiKeys(PageNumberBasedPaginationSpec.MAX_PAGE_SIZE_SPEC);
    return apiKeys.getPageContents();
  }

  /** Finds an API key by its key ID (not the database ID) directly from the database. */
  public Optional<ApiKeyModel> findByKeyId(String keyId) {
    return repository.lookupApiKey(keyId).toCompletableFuture().join();
  }

  /**
   * Finds an API key by its key ID (not the database ID). Checks the API key cache first and
   * queries the database if the key isn't cache. Lookups for invalid key IDs are cached with a
   * value of Optional.empty() to relieve database pressure caused by repeated invalid requests.
   */
  public Optional<ApiKeyModel> findByKeyIdWithCache(String keyId) {
    return apiKeyCache.getOrElseUpdate(
        keyId, () -> findByKeyId(keyId), CACHE_EXPIRATION_TIME_SECONDS);
  }

  /** Increment an API key's call count and set its last call IP address to the one provided. */
  public void recordApiKeyUsage(String apiKeyId, String remoteAddress) {
    repository.recordApiKeyUsage(apiKeyId, remoteAddress);
  }

  /**
   * Marks an {@link ApiKeyModel} as retired, resulting in all requests that use it to fail
   * authentication. Retiring is permanent.
   */
  public ApiKeyModel retireApiKey(Long apiKeyId, CiviFormProfile profile) {
    Optional<ApiKeyModel> maybeApiKey =
        repository.lookupApiKey(apiKeyId).toCompletableFuture().join();

    if (maybeApiKey.isEmpty()) {
      throw new RuntimeException(new ApiKeyNotFoundException(apiKeyId));
    }

    ApiKeyModel apiKey = maybeApiKey.get();

    if (apiKey.isRetired()) {
      throw new NotChangeableException(String.format("ApiKey %s is already retired", apiKey));
    }

    apiKey.retire(getAuthorityId(profile));
    apiKey.save();

    return apiKey;
  }

  /**
   * Creates a new {@link ApiKeyModel} with the data in {@code form} if it passes validation.
   *
   * @param form Stores data specified by the user for creation of the key, as well as validation
   *     errors
   * @param profile The user profile of the account creating this API key, used for recording who
   *     created it
   * @return a result object containing the credentials and created key if successful, form with
   *     validation messages if not
   */
  public ApiKeyCreationResult createApiKey(DynamicForm form, CiviFormProfile profile) {
    if (environment.isProd() && secretSalt.equals(DEFAULT_API_KEY_SECRET_SALT)) {
      throw new RuntimeException("Must set api_secret_salt in production environment.");
    }

    ApiKeyGrants grants = resolveGrants(form);

    if (grants.getProgramGrants().isEmpty()) {
      form = form.withError(PROGRAMS_FIELD_GROUP_NAME, "");
    }

    ApiKeyModel apiKey = new ApiKeyModel(grants);

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

    apiKey.setKeyId(keyId);
    apiKey.setSaltedKeySecret(saltedSecret);
    apiKey.setCreatedBy(getAuthorityId(profile));

    apiKey = repository.insert(apiKey).toCompletableFuture().join();

    return ApiKeyCreationResult.success(apiKey, keyId, keySecret);
  }

  // {@code apiKey} is mutable and modified here, {@code form} is immutable so a new instance is
  // returned.
  private DynamicForm resolveKeyName(DynamicForm form, ApiKeyModel apiKey) {
    String keyName = form.rawData().getOrDefault(FORM_FIELD_NAME_KEY_NAME, "");

    if (!keyName.isBlank()) {
      apiKey.setName(keyName);
    } else {
      form = form.withError(FORM_FIELD_NAME_KEY_NAME, "Key name cannot be blank.");
    }

    return form;
  }

  // apiKey is mutable and modified here, form is immutable so a new instance is returned
  private DynamicForm resolveExpiration(DynamicForm form, ApiKeyModel apiKey) {
    String expirationString = form.rawData().getOrDefault(FORM_FIELD_NAME_EXPIRATION, "");

    if (expirationString.isBlank()) {
      return form.withError(FORM_FIELD_NAME_EXPIRATION, "Expiration cannot be blank.");
    }

    try {
      Instant expiration =
          dateConverter.parseIso8601DateToStartOfLocalDateInstant(expirationString);
      apiKey.setExpiration(expiration);
    } catch (DateTimeParseException e) {
      return form.withError(
          FORM_FIELD_NAME_EXPIRATION, "Expiration must be in the form YYYY-MM-DD.");
    }

    return form;
  }

  // apiKey is mutable and modified here, form is immutable so a new instance is returned
  private DynamicForm resolveSubnet(DynamicForm form, ApiKeyModel apiKey) {
    String subnetInputString = form.rawData().getOrDefault(FORM_FIELD_NAME_SUBNET, "");
    subnetInputString = subnetInputString.trim();

    if (subnetInputString.isBlank()) {
      return form.withError(FORM_FIELD_NAME_SUBNET, "Subnet cannot be blank.");
    }

    for (String subnetString : Splitter.on(",").split(subnetInputString)) {
      try {
        SubnetUtils subnetUtils = new SubnetUtils(subnetString);

        if (subnetUtils.getInfo().getNetmask().equals(GLOBAL_SUBNET_NETMASK) && banGlobalSubnet) {
          return form.withError(FORM_FIELD_NAME_SUBNET, "Subnet cannot allow all IP addresses.");
        }
      } catch (IllegalArgumentException e) {
        return form.withError(FORM_FIELD_NAME_SUBNET, "Subnet must be in CIDR notation.");
      }
    }

    apiKey.setSubnet(subnetInputString);

    return form;
  }

  // Pattern for matching and extracting form field names that specify
  // granting read permission for a program.
  // These field names have the format "grant-program-read[program-slug]"
  // Where "program-slug" is the sluggified name of the program the key should
  // be granted read access for e.g. "grant-program-read[utility-discount-program]".
  private static final Pattern GRANT_PROGRAM_READ_PATTERN =
      Pattern.compile("^grant-program-read\\[([\\w\\-]+)\\]$");

  private ApiKeyGrants resolveGrants(DynamicForm form) {
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
        // Making the root exception cause a ProgramNotFoundException will cause
        // controllers.ErrorHandler to handle it
        // as a client-error, rather than server-error.
        throw new RuntimeException(new ProgramNotFoundException(programSlug));
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

  /**
   * Apply the HMAC-SHA-256 hashing function to the input using the "api_secret_salt" config value
   * as a key.
   */
  public String salt(String message) {
    return CryptographicUtils.sign(message, secretSalt);
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
}
