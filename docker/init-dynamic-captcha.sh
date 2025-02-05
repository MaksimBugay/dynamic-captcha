#!/bin/bash

docker service rm backend-local_dynamic-captcha
docker config rm dynamic-captcha-config-0

(DEPLOY_VERSION=2 docker stack deploy --with-registry-auth -c backend.yml backend-local)
