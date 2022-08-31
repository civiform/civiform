import Provider from 'oidc-provider'
const configuration = {
  clients: [
    {
      client_id: 'foo',
      client_secret: 'bar',
      response_types: ['id_token'],
      response_mode: ['form_post'],
      grant_types: ['implicit'],
      application_type: 'web',
      scopes: ['openid', 'profile'],
      redirect_uris: [
        'http://localhost:9000/callback/OidcClient',
        'http://localhost:9000/callback/AdClient',
        'http://localhost:19001/callback/OidcClient',
        'http://localhost:19001/callback/AdClient',
        'http://civiform:9000/callback/OidcClient',
        'http://civiform:9000/callback/AdClient',
      ],
    },
  ],

  // Required method, we fake the account details.
  async findAccount(ctx, id) {
    return {
      accountId: id,
      async claims() {
        return {
          sub: id,
          // pretend to be IDCS which uses this key for user email.
          user_emailid: id + '@example.com',
          // lie about verification for tests.
          email_verified: true,
          user_displayname: 'Test middlename User',
        }
      },
    }
  },
  claims: {
    openid: ['sub'],
    email: ['user_emailid', 'email_verified', 'user_displayname'],
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

oidc.listen(oidcPort, () => {
  console.log(
    `oidc-provider listening on port ${oidcPort}, check http://localhost:${oidcPort}/.well-known/openid-configuration`,
  )
})
