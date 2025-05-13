#! /usr/bin/env bash

echo "health-check"
curl --silent http://localhost:8000/api-bridge/health-check | jq
echo ""

echo "discovery"
curl --silent http://localhost:8000/api-bridge/discovery | jq
echo ""

echo "bridge error"
curl --silent -X POST -H "Content-Type: application/json" http://localhost:8000/api-bridge/bridge/error --data '{"payload": {}}' | jq
echo ""

echo "bridge fail-validation"
curl --silent -X POST -H "Content-Type: application/json" http://localhost:8000/api-bridge/bridge/fail-validation --data '{"payload": {}}' | jq
echo ""

echo "bridge success - valid"
curl --silent -X POST -H "Content-Type: application/json" http://localhost:8000/api-bridge/bridge/success --data '{
  "payload": {
    "accountNumber": 1234
  }
}' | jq
echo ""

echo "bridge success - invalid"
curl --silent -X POST -H "Content-Type: application/json" http://localhost:8000/api-bridge/bridge/success --data '{
  "payload": {
    "accountNumber": 1111
  }
}' | jq
echo ""
