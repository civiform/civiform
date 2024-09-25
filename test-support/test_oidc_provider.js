import Provider from 'oidc-provider'
const configuration = {
  clients: [
    {
      // Normal OIDC
      client_id: 'generic-fake-oidc-client',
      client_secret: 'generic-fake-oidc-secret',
      response_types: ['id_token', 'id_token token'],
      response_mode: ['form_post'],
      grant_types: ['implicit'],
      application_type: 'web',
      scopes: ['openid', 'profile', 'phone'],
      // Note: The invalidate function is overridden on the server in order to allow redirect URLs
      // on the localhost domain as well as an insecure domain.
      redirect_uris: [
        // Redirects for localhost are used when the server is being run locally on a dev machine.
        'http://localhost:9000/callback/OidcClient',
        'http://localhost:9000/callback/generic-oidc',
        'http://localhost:9000/callback/AdClient',
        'http://localhost:9000',
        'http://localhost:19001/callback/OidcClient',
        'http://localhost:19001/callback/generic-oidc',
        'http://localhost:19001/callback/AdClient',
        'http://localhost:19001',
        // Redirects for the "civiform" host are used when the server is being run within browser
        // tests.
        'http://civiform:9000/callback/OidcClient',
        'http://civiform:9000/callback/generic-oidc',
        'http://civiform:9000/callback/AdClient',
        'http://civiform:9000',
        // Local browser tests
        'http://localhost:9999/callback/OidcClient',
        'http://localhost:9999/callback/generic-oidc',
        'http://localhost:9999/callback/AdClient',
        'http://localhost:9999',
      ],
      post_logout_redirect_uris: [
        'http://localhost:9000/',
        'http://localhost:19001/',
        'http://civiform:9000/',
        'http://localhost:9999/',
      ],
    },
    {
      // IDCS
      client_id: 'idcs-fake-oidc-client',
      client_secret: 'idcs-fake-oidc-secret',
      response_types: ['id_token'],
      response_mode: ['form_post'],
      grant_types: ['implicit'],
      application_type: 'web',
      scopes: ['openid', 'profile', 'phone'],
      // Note: The invalidate function is overridden on the server in order to allow redirect URLs
      // on the localhost domain as well as an insecure domain.
      redirect_uris: [
        // Redirects for localhost are used when the server is being run locally on a dev machine.
        'http://localhost:9000/callback/OidcClient',
        'http://localhost:9000/callback/generic-oidc',
        'http://localhost:9000/callback/AdClient',
        'http://localhost:9000',
        'http://localhost:19001/callback/OidcClient',
        'http://localhost:19001/callback/generic-oidc',
        'http://localhost:19001/callback/AdClient',
        'http://localhost:19001',
        // Redirects for the "civiform" host are used when the server is being run within browser
        // tests.
        'http://civiform:9000/callback/OidcClient',
        'http://civiform:9000/callback/generic-oidc',
        'http://civiform:9000/callback/AdClient',
        'http://civiform:9000',
        // Local browser tests
        'http://localhost:9999/callback/OidcClient',
        'http://localhost:9999/callback/generic-oidc',
        'http://localhost:9999/callback/AdClient',
        'http://localhost:9999',
      ],
      post_logout_redirect_uris: [
        'http://localhost:9000/',
        'http://localhost:19001/',
        'http://civiform:9000/',
        'http://localhost:9999/',
      ],
    },
  ],
  // Required method, we fake the account details.
  async findAccount(ctx, id) {
    const email = `${id}@example.com`
  
    return {
      accountId: id,
      async claims(use, scope) {
        const claims = {
          sub: id,
          // lie about verification for tests.
          email_verified: true
        };

        if (ctx.oidc.client.clientId == 'idcs-fake-oidc-client') {
          // pretend to be IDCS which uses this key for user email.
          claims.user_emailid = email;
          // The display name is what appears in the UI when logged in. The CiviForm server
          // supports display name with multiple components (e.g. "first middle last") as well as a
          // single component (e.g. foo@example.com). For simplicity, the email address is used
          // since there's only a single "id" field available at this point.
          claims.user_displayname = email;
        } else {
          claims.email = email;
          claims.name = email;
          claims.picture = 'https://www.gravatar.com/avatar/00000000000000000000000000000000.png';
        }


        // Include if 'phone' scope is requested
        if (scope.includes('phone')) {
          // Using area code 253 so that it passes the phone validation
          claims.phone_number = '2538675309';
          claims.phone_number_verified = true;
        }

        return claims;
      },
    }
  },
  claims: {
    openid: ['sub'],
    email: ['user_emailid', 'email_verified', 'email'],
    profile: ['name', 'picture', 'user_displayname'],
    phone: ['phone_number', 'phone_number_verified']
  },
  responseTypes: [
    'code id_token token',
    'code id_token',
    'code token',
    'code',
    'id_token token',
    'id_token',
    'none',
  ],
  features: {
    rpInitiatedLogout: {
      enabled: true,
    },
  },
}

const oidcPort = process.env.OIDC_PORT || 3380
const oidc = new Provider('http://localhost:' + oidcPort, configuration)

const {invalidate: orig} = oidc.Client.Schema.prototype

// By default, redirect URLs must be https and cannot refer to localhost. While these are correct
// for production implementations, this is intended to be used ONLY for testing. For browser tests,
// internal Docker networking is used, which uses HTTP. For running the server locally, redirects
// are pointed towards localhost.
// From https://github.com/panva/node-oidc-provider/blob/0fcc112e0a95b3b2dae4eba6da812253277567c9/recipes/implicit_http_localhost.md
oidc.Client.Schema.prototype.invalidate = function invalidate(message, code) {
  if (code === 'implicit-force-https' || code === 'implicit-forbid-localhost') {
    return
  }
  orig.call(this, message)
}

process.on('SIGINT', () => {
  console.info('Interrupted')
  process.exit(0)
})

// This will print any server errors out. Otherwise there's no visibility into what's going on.
oidc.on('server_error', (ctx, error) => {
  console.error('Server error occurred:', error);
});

oidc.listen(oidcPort, () => {
  console.log(
    `oidc-provider listening on port ${oidcPort}, check http://localhost:${oidcPort}/.well-known/openid-configuration`,
  )
})
