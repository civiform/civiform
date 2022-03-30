# Dev Azure
This is a deploy directory that allows individuals to create a deploy of the 
app. We intentionally don't use a shared state with this terraform directory
to allow for everyone to have their own app. 

# Running the dev deploy
## Copy the civiform_config.example.sh 
Copy the civiform_config.example.sh into civiform_config.sh and
 change the required variables. 

## Get access to correct technologies
You will need to reach out to team members to get accounts for the following:
- Azure
- AWS
- Login Radius Civiform-Staging 

## Setup Local Machine
Run through the doctor script to make sure you have the right things on 
your machine: 

```
cloud/shared/bin/doctor
```

## Setup Login Radius For Local Development
Go to [Login Radius Dashboard](https://dashboard.loginradius.com/) and click
configure a civiform integration. Choose the outbound SSO Saml.

From there add an app with Sp initiated login and pick a name (this gets put
into the config as `LOGIN_RADIUS_SAML_APP_NAME`).

To generate the private key for the form run, cat the file and put into dashboard.
```
openssl genrsa -out private.key 2048
```

For generating the cert run, cat the file and put into dashboard.
```
openssl req -new -x509 -key private.key -out certificate.cert -days 365 -subj /CN=civiform-staging.hub.loginradius.com
```

We need to copy the details from a previous working setup in login radius 
once we set up the certs so look back at the staging one to fill out.

## Run
After that you can start the setup by running and following the instructions:

```
cloud/deploys/dev_azure/bin/setup --tag=<IMAGE_TAG>
```

# Local Docker Build to Remote Azure Deploy
If you want to do local onto terraform we build/tag/deploy the docker image 
and then update the azure app service to point to the local image. 

## 1. Build, Tag and Push the Docker Image
Run the following script which takes the DOCKER_REPOSITORY, 
DOCKER_USERNAME from your civiform_config.sh and builds/tags/pushes your local 
up to docker hub. You must specify the image_tag to use for docker.
You will need a custom docker hub in order to do this. 
Check with team on how to pay for docker hub pro.

```
cloud/deploys/dev_azure/bin/docker-build-tag-push --tag=<IMAGE_TAG>
```

## 2. Deploy the new version 

Deploy via the deploy script 
```
cloud/deploys/dev_azure/bin/deploy --tag=<IMAGE_TAG>
```
or Within the app service resource, you can select Deployment Center, and within
the registry settings change the 'Full Image Name and Tag' to be (the image_tag
is what you specified in the build/tag/push)
`<DOCKER_USERNAME>/<DOCKER_REPOSITORY>:<IMAGE_TAG>`
