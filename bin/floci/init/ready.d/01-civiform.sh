#!/bin/sh

# Floci ready hook, mounted into the floci container. Creates the S3 buckets and
# verifies the SES sender that CiviForm uses in dev and browser tests. Floci
# rejects SendEmail from an unverified sender, and keeps state in memory, so
# this has to run on every container start.
#
# The bucket names match bin/floci/lib.sh and the sender default matches
# server/conf/helper/email.conf.

set -eu

region="us-west-2"
sender="${SENDER_EMAIL_ADDRESS:-noreply@fake.identity}"

for bucket in civiform-local-s3 civiform-local-s3-public; do
  if ! aws s3api head-bucket --bucket "${bucket}" --region "${region}" >/dev/null 2>&1; then
    aws s3api create-bucket \
      --bucket "${bucket}" \
      --region "${region}" \
      --create-bucket-configuration "LocationConstraint=${region}" >/dev/null
  fi
done

aws ses verify-email-identity --region "${region}" --email-address "${sender}"

echo "CiviForm buckets created and ${sender} verified"
