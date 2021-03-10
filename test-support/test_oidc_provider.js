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
    scopes: ['openid'],
    redirect_uris: ['http://localhost:9000/callback/OidcClient', 'http://localhost:9000/callback/AdClient', 'http://localhost:19001/callback/OidcClient', 'http://localhost:19001/callback/AdClient'],
  }],
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
