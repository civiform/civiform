const { Provider } = require('oidc-provider');
const configuration = {
  clients: [{
    client_id: 'foo',
    client_secret: 'bar',
    response_types: ['id_token'],
    response_mode: ['form_post'],
    grant_types: ['implicit'],
    // "native" because we're on localhost.
    application_type: 'native',
    scopes: ['openid', 'profile'],
    redirect_uris: ['http://localhost:9000/callback/OidcClient', 'http://localhost:9000/callback/AdClient', 'http://localhost:19001/callback/OidcClient', 'http://localhost:19001/callback/AdClient'],
  }],

  // Required method, we fake the account details.
  async findAccount(ctx, id) {
    return {
      accountId: id,
      async claims() {
        return {
          sub: id,
          // pretend to be IDCS which uses this key for user email.
          user_emailid: id + "@example.com",
          // lie about verification for tests.
          email_verified: true,
          user_displayname: "first middle last"
        };
      },
    };
  },
  claims: {
    openid: ['sub'],
    email: ['user_emailid', 'email_verified', 'user_displayname'],
  }
};

const oidc = new Provider('http://localhost:3380', configuration);

var process = require('process');
process.on('SIGINT', () => {
  console.info("Interrupted")
  process.exit(0)
});

const server = oidc.listen(3380, () => {
  console.log('oidc-provider listening on port 3380, check http://localhost:3380/.well-known/openid-configuration');
});
