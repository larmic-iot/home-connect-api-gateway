.DEFAULT_GOAL := help

# Container/image naming
CONTAINER_NAME ?= home-connect-api-gateway
IMAGE_NAME ?= larmic/home-connect-api-gateway
IMAGE_TAG ?= latest

.PHONY: help update-assets build-binary docker-build-arm docker-build-amd gradle-run run-local docker-run docker-stop

help: ## Outputs this help screen
	@grep -E '(^[a-zA-Z0-9_-]+:.*?##.*$$)|(^##)' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}{printf "\033[32m%-30s\033[0m %s\n", $$1, $$2}' | sed -e 's/\[32m##/[33m/'

## —— Assets 🏗️——————————————————————————————————————————————————————————————————————————————————————————————————
# TODO After renovate update this task must be run manually
STOPLIGHT_VERSION := 9.0.6
ASSETS_DIR := resources/assets

update-stoplight-assets:
	@echo "🔄 Update frontend assets..."
	@mkdir -p $(ASSETS_DIR)/stoplight-elements
	@echo "📦 Loading Stoplight Elements v$(STOPLIGHT_VERSION)..."
	@curl -L --fail -o "$(ASSETS_DIR)/stoplight-elements/styles.min.css" "https://unpkg.com/@stoplight/elements@$(STOPLIGHT_VERSION)/styles.min.css"
	@curl -L --fail -o "$(ASSETS_DIR)/stoplight-elements/web-components.min.js" "https://unpkg.com/@stoplight/elements@$(STOPLIGHT_VERSION)/web-components.min.js"
	@echo "✅ Update assets successful!"

## —— Build 🏗️———————————————————————————————————————————————————————————————————————————————————————————————————
build-binary: ## Builds binary (executable) in ./build/bin/app/releaseExecutable/app.(k)exe (uses architecture of local machine)
	./gradlew linkReleaseExecutableApp

docker-build-arm: ## Builds ARM64 docker image (linux/arm64)
	@echo "Remove docker image if already exists"
	-docker rmi -f ${IMAGE_NAME}:${IMAGE_TAG}
	@echo "Build docker image"
	DOCKER_BUILDKIT=1 docker buildx build --platform linux/arm64 -t ${IMAGE_NAME}:${IMAGE_TAG} --load .
	@echo "Prune intermediate images"
	-docker image prune -f

docker-build-amd: ## Builds AMD64 docker image (linux/amd64)
	@echo "Remove docker image if already exists"
	-docker rmi -f ${IMAGE_NAME}:${IMAGE_TAG}
	@echo "Build docker image"
	DOCKER_BUILDKIT=1 docker buildx build --platform linux/amd64 -t ${IMAGE_NAME}:${IMAGE_TAG} --load .
	@echo "Prune intermediate images"
	-docker image prune -f

## —— Run application 🏃🏽————————————————————————————————————————————————————————————————————————————————————————
# Include .env file if it exists (for local development)
-include .env
export

gradle-run: ## Runs app without any container
	./gradlew runDebug

run-local:
	./build/bin/app/releaseExecutable/app.kexe

docker-run: ## Runs docker container and tails logs (exposes 8080)
	@echo "Run docker container"
	docker run -p 8080:8080 --env-file .env --rm --name ${CONTAINER_NAME} ${IMAGE_NAME}:${IMAGE_TAG}
	docker logs -f ${CONTAINER_NAME}

docker-stop: ## Stops running docker container
	@echo "Stop docker container"
	docker stop ${CONTAINER_NAME}