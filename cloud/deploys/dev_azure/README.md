# Dev Azure
This is a deploy directory that allows individuals to create a deploy of the 
app. We intentionally don't use a shared state with this terraform directory
to allow for everyone to have their own app. 

The process to deploy is similar
to the staging process but we do not have to do the shared state set up. It 
should be `terraform init` and then `terraform apply`

# Local Docker Build to Remote Azure Deploy
If you want to do local onto terraform we build/tag/deploy the docker image 
and then update the azure app service to point to the local image. 

## 1. Build, Tag and Push the Docker Image
This should take like 30 minutes (the push takes the longest).

```
docker build -f prod.Dockerfile -t <IMAGE_TAG> --cache-from docker.io/civiform/civiform-browser-test:latest --build-arg BUILDKIT_INLINE_CACHE=1 .
docker tag <IMAGE_TAG> <DOCKER_USERNAME>/<DOCKER_REPO_NAME>:<IMAGE_TAG>
docker push <DOCKER_USERNAME>/<DOCKER_REPO_NAME>:<IMAGE_TAG>
```

## 2. Update the image name/tag for your remote azure deploy
You can do this one of two ways. Terraform deploy or update via the azure 
portal 

### Update via the .tfvars File and deploy
After that you can change your .tfvars file to point to the docker tag you set

```
docker_repository_name = <DOCKER_REPO_NAME>
docker_username        = <DOCKER_USERNAME>
```

deploy via `terraform apply`

### Update via the azure portal
Within the app service resource, you can select Deployment Center, and within
the registry settings change the 'Full Image Name and Tag' to be 
`<DOCKER_USERNAME>/<DOCKER_REPO_NAME>:<IMAGE_TAG>`
