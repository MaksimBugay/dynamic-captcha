#!/bin/bash
docker login -p=sec^GHRmfZu7uTv -u=n7fr846yfa6ohlhe
docker buildx build --platform linux/amd64 -t n7fr846yfa6ohlhe/mbugai:dynamic-captcha-$1 -f Dockerfile .
docker push n7fr846yfa6ohlhe/mbugai:dynamic-captcha-$1

read -p "Press any key..."