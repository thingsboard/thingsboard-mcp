#!/usr/bin/env bash

# Build and publish multi-arch Docker image for ThingsBoard MCP Server
# Default image: thingsboard/mcp
# Usage examples:
#   ./build-and-publish.sh                 # builds and pushes tag 'latest'
#   ./build-and-publish.sh 1.0.0           # builds and pushes tag '1.0.0'
#   ./build-and-publish.sh 1.0.0 --push-latest
#   ./build-and-publish.sh --no-cache --platforms linux/amd64,linux/arm64
#
# Optional env for non-interactive login:
#   DOCKER_USERNAME=your_user DOCKER_PASSWORD=your_pass ./build-and-publish.sh 1.0.0
#   DOCKER_USERNAME=your_user DOCKERHUB_TOKEN=your_token ./build-and-publish.sh 1.0.0

set -euo pipefail

IMAGE="thingsboard/mcp"
TAG="latest"
PLATFORMS="linux/amd64,linux/arm64"
BUILDER_NAME="tb-builder"
PUSH_LATEST="false"
NO_CACHE="false"

usage() {
  cat <<'EOF'
Build and publish multi-arch Docker image for ThingsBoard MCP Server.

Usage:
  $(basename "$0") [TAG] [options]

Arguments:
  TAG                   Image tag to publish (default: latest)

Options:
  --push-latest         Also push the 'latest' tag when TAG != latest
  --no-cache            Do not use cache when building the image
  --platforms LIST      Comma-separated platforms (default: ${PLATFORMS})
  --builder NAME        Docker buildx builder name (default: ${BUILDER_NAME})
  -h, --help            Show this help message

Environment variables for login (optional):
  DOCKER_USERNAME       Docker registry username (Docker Hub)
  DOCKER_PASSWORD       Docker password (mutually exclusive with DOCKERHUB_TOKEN)
  DOCKERHUB_TOKEN       Docker Hub access token (preferred over password)

Examples:
  ${0}                  # build+push thingsboard/mcp:latest
  ${0} 1.0.0            # build+push thingsboard/mcp:1.0.0
  ${0} 1.0.0 --push-latest
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "${1}" in
    -h|--help)
      usage; exit 0 ;;
    --push-latest)
      PUSH_LATEST="true"; shift ;;
    --no-cache)
      NO_CACHE="true"; shift ;;
    --platforms)
      [[ $# -ge 2 ]] || { echo "Error: --platforms requires a value" >&2; exit 1; }
      PLATFORMS="$2"; shift 2 ;;
    --builder)
      [[ $# -ge 2 ]] || { echo "Error: --builder requires a value" >&2; exit 1; }
      BUILDER_NAME="$2"; shift 2 ;;
    *)
      # First non-flag argument is the TAG
      TAG="$1"; shift ;;
  esac
done

echo "[INFO] Image:       ${IMAGE}"
echo "[INFO] Tag:         ${TAG}"
echo "[INFO] Platforms:   ${PLATFORMS}"
echo "[INFO] Builder:     ${BUILDER_NAME}"
echo "[INFO] Push latest: ${PUSH_LATEST}"
echo "[INFO] No cache:    ${NO_CACHE}"

# Optional Docker login (non-interactive) if credentials are provided
if [[ -n "${DOCKER_USERNAME:-}" ]] && { [[ -n "${DOCKERHUB_TOKEN:-}" ]] || [[ -n "${DOCKER_PASSWORD:-}" ]]; }; then
  echo "[INFO] Logging in to Docker Hub as ${DOCKER_USERNAME} (non-interactive)"
  TOKEN_OR_PASS="${DOCKERHUB_TOKEN:-}"
  if [[ -z "${TOKEN_OR_PASS}" ]]; then
    TOKEN_OR_PASS="${DOCKER_PASSWORD}"
  fi
  echo "${TOKEN_OR_PASS}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
else
  echo "[INFO] Skipping non-interactive docker login (no credentials provided). Ensure you are logged in if pushing to a private namespace."
fi

# Ensure buildx builder exists and is selected
if docker buildx inspect "${BUILDER_NAME}" >/dev/null 2>&1; then
  docker buildx use "${BUILDER_NAME}"
else
  echo "[INFO] Creating buildx builder '${BUILDER_NAME}'"
  docker buildx create --name "${BUILDER_NAME}" --use >/dev/null
fi

# Build args
BUILD_ARGS=(
  "--platform" "${PLATFORMS}"
  "-t" "${IMAGE}:${TAG}"
  "--push"
)

if [[ "${PUSH_LATEST}" == "true" && "${TAG}" != "latest" ]]; then
  BUILD_ARGS+=("-t" "${IMAGE}:latest")
fi

if [[ "${NO_CACHE}" == "true" ]]; then
  BUILD_ARGS+=("--no-cache")
fi

echo "[INFO] Building and pushing image..."
set -x
docker buildx build "${BUILD_ARGS[@]}" .
set +x

echo "[INFO] Done. Pushed: ${IMAGE}:${TAG}"
if [[ "${PUSH_LATEST}" == "true" && "${TAG}" != "latest" ]]; then
  echo "[INFO] Also pushed: ${IMAGE}:latest"
fi
