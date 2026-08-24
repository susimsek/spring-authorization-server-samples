#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(pwd)"
FRONTEND_DIR="$PROJECT_DIR/src/main/frontend"
BUNDLE_DIR="$PROJECT_DIR/.offline-bundle"
DESKTOP="${HOME}/Desktop"
OUTPUT="$DESKTOP/spring-auth-build-dependencies-linux-x64.zip"
MAVEN_VERSION="3.9.16"
GRAAL_VERSION="25.0.2"
export CI=true

fail() { echo "ERROR: $*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || fail "$1 bulunamadı."; }

[[ -f "$PROJECT_DIR/pom.xml" ]] || fail "pom.xml bulunamadı. Scripti proje root dizininde çalıştır."
[[ -f "$FRONTEND_DIR/package.json" ]] || fail "Frontend package.json bulunamadı."
[[ -f "$FRONTEND_DIR/pnpm-lock.yaml" ]] || fail "pnpm-lock.yaml bulunamadı."
for c in curl tar zip unzip java node npm corepack; do need "$c"; done

PACKAGE_MANAGER="$(cd "$FRONTEND_DIR" && node -p "require('./package.json').packageManager || ''")"
[[ "$PACKAGE_MANAGER" == pnpm@* ]] || fail "packageManager pnpm değil: $PACKAGE_MANAGER"
PNPM_VERSION="${PACKAGE_MANAGER#pnpm@}"
NEXT_VERSION="$(cd "$FRONTEND_DIR" && node -p "require('./package.json').dependencies.next")"
CYPRESS_VERSION="$(cd "$FRONTEND_DIR" && node -p "const p=require('./package.json'); const v=(p.devDependencies&&p.devDependencies.cypress)||(p.dependencies&&p.dependencies.cypress)||''; v.replace(/^[^0-9]*/,'')")"
[[ -n "$CYPRESS_VERSION" ]] || fail "Cypress version package.json içinden okunamadı."
NODE_VERSION="$(grep -o '<node.version>[^<]*</node.version>' "$PROJECT_DIR/pom.xml" | head -1 | sed 's#<node.version>##;s#</node.version>##;s/^v//')"
[[ -n "$NODE_VERSION" ]] || fail "node.version pom.xml içinden okunamadı."

echo "Node=$NODE_VERSION pnpm=$PNPM_VERSION Next=$NEXT_VERSION Cypress=$CYPRESS_VERSION Maven=$MAVEN_VERSION GraalVM=$GRAAL_VERSION"

NODE_ARCHIVE="node-v${NODE_VERSION}-linux-x64.tar.gz"
NODE_URL="https://nodejs.org/dist/v${NODE_VERSION}/${NODE_ARCHIVE}"
GRAAL_ARCHIVE="graalvm-community-jdk-${GRAAL_VERSION}_linux-x64_bin.tar.gz"
GRAAL_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAAL_VERSION}/${GRAAL_ARCHIVE}"
MAVEN_ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/${MAVEN_ARCHIVE}"

cd "$PROJECT_DIR"
rm -rf "$BUNDLE_DIR"
mkdir -p "$BUNDLE_DIR"/{runtime,bin,native-packages,maven-repository,pnpm-store,cypress,linux-debs,linux-root}

# Linux Node
cd "$BUNDLE_DIR/runtime"
curl -fL --retry 5 --retry-delay 2 --connect-timeout 30 -o "$NODE_ARCHIVE" "$NODE_URL"
tar -xzf "$NODE_ARCHIVE" && rm -f "$NODE_ARCHIVE"
NODE_LINUX_HOME="$BUNDLE_DIR/runtime/node-v${NODE_VERSION}-linux-x64"
[[ -x "$NODE_LINUX_HOME/bin/node" ]] || fail "Linux Node bulunamadı."

# Linux GraalVM
cd "$BUNDLE_DIR/runtime"
curl -fL --retry 5 --retry-delay 2 --connect-timeout 30 -o "$GRAAL_ARCHIVE" "$GRAAL_URL"
tar -xzf "$GRAAL_ARCHIVE" && rm -f "$GRAAL_ARCHIVE"
GRAAL_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name "graalvm-community-openjdk-${GRAAL_VERSION}*" | head -1)"
[[ -n "$GRAAL_HOME" && -x "$GRAAL_HOME/bin/java" && -x "$GRAAL_HOME/bin/native-image" ]] || fail "GraalVM/native-image bulunamadı."

# Maven runtime
cd "$BUNDLE_DIR/runtime"
curl -fL --retry 5 --retry-delay 2 --connect-timeout 30 -o "$MAVEN_ARCHIVE" "$MAVEN_URL"
tar -xzf "$MAVEN_ARCHIVE" && rm -f "$MAVEN_ARCHIVE"
MAVEN_HOME="$BUNDLE_DIR/runtime/apache-maven-${MAVEN_VERSION}"
[[ -x "$MAVEN_HOME/bin/mvn" ]] || fail "Maven runtime bulunamadı."

# pnpm runtime package
PNPM_DOWNLOAD_DIR="$BUNDLE_DIR/runtime/.pnpm-download"
PNPM_HOME="$BUNDLE_DIR/runtime/pnpm-${PNPM_VERSION}"
rm -rf "$PNPM_DOWNLOAD_DIR" "$PNPM_HOME"
mkdir -p "$PNPM_DOWNLOAD_DIR" "$PNPM_HOME"
cd "$PNPM_DOWNLOAD_DIR"
PNPM_TGZ_NAME="$(npm pack --silent "pnpm@${PNPM_VERSION}" | tail -1)"
[[ -f "$PNPM_TGZ_NAME" ]] || fail "pnpm package indirilemedi."
tar -xzf "$PNPM_TGZ_NAME" -C "$PNPM_HOME" --strip-components=1
cd "$PROJECT_DIR"
rm -rf "$PNPM_DOWNLOAD_DIR"
[[ -f "$PNPM_HOME/bin/pnpm.cjs" ]] || fail "pnpm.cjs bulunamadı."

# Local pnpm for bundle preparation
corepack enable || true
corepack prepare "pnpm@${PNPM_VERSION}" --activate

# Next Linux SWC packages
cd "$BUNDLE_DIR/native-packages"
SWC_GNU_TGZ_NAME="$(npm pack --silent "@next/swc-linux-x64-gnu@${NEXT_VERSION}" | tail -1)"
SWC_MUSL_TGZ_NAME="$(npm pack --silent "@next/swc-linux-x64-musl@${NEXT_VERSION}" | tail -1)"
SWC_GNU_TGZ="$BUNDLE_DIR/native-packages/$SWC_GNU_TGZ_NAME"
SWC_MUSL_TGZ="$BUNDLE_DIR/native-packages/$SWC_MUSL_TGZ_NAME"
[[ -f "$SWC_GNU_TGZ" && -f "$SWC_MUSL_TGZ" ]] || fail "Next Linux SWC paketleri indirilemedi."

# Cypress Linux x64 binary
CYPRESS_RUNTIME="$BUNDLE_DIR/cypress/$CYPRESS_VERSION"
CYPRESS_ZIP="$BUNDLE_DIR/cypress/cypress-${CYPRESS_VERSION}-linux-x64.zip"
mkdir -p "$CYPRESS_RUNTIME"
curl -fL --retry 5 --retry-delay 2 --connect-timeout 30 "https://download.cypress.io/desktop/${CYPRESS_VERSION}?platform=linux&arch=x64" -o "$CYPRESS_ZIP"
unzip -q "$CYPRESS_ZIP" -d "$CYPRESS_RUNTIME"
rm -f "$CYPRESS_ZIP"
CYPRESS_BINARY="$(find "$CYPRESS_RUNTIME" -type f -path '*/Cypress/Cypress' | head -1)"
[[ -n "$CYPRESS_BINARY" ]] || fail "Cypress Linux binary bulunamadı."
chmod +x "$CYPRESS_BINARY"

# Ubuntu 24.04 Cypress/Xvfb system packages
CONTAINER_BIN=""
if command -v podman >/dev/null 2>&1; then CONTAINER_BIN=podman; elif command -v docker >/dev/null 2>&1; then CONTAINER_BIN=docker; else fail "Cypress Linux sistem paketleri için Docker veya Podman gerekli."; fi
rm -rf "$BUNDLE_DIR/linux-debs" && mkdir -p "$BUNDLE_DIR/linux-debs"
"$CONTAINER_BIN" run --rm --platform linux/amd64 -v "$BUNDLE_DIR/linux-debs:/out" ubuntu:24.04 bash -c '
  set -e
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y --download-only \
    xvfb xauth x11-xkb-utils libgtk-3-0t64 libgbm-dev libnotify-dev \
    libnss3 libxss1 libasound2t64 libxtst6
  cp /var/cache/apt/archives/*.deb /out/
'
[[ "$(find "$BUNDLE_DIR/linux-debs" -type f -name '*.deb' | wc -l | tr -d ' ')" != "0" ]] || fail "Linux .deb paketleri indirilemedi."

# Maven project repository
cd "$PROJECT_DIR"
MAVEN_LOCAL_REPO="$BUNDLE_DIR/maven-repository"
"$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" -Dskip.frontend=true dependency:go-offline
"$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" -Dskip.frontend=true -DskipTests package
"$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" -Dskip.frontend=true test

# Frontend project store and portable node_modules
cd "$FRONTEND_DIR"
rm -rf "$BUNDLE_DIR/pnpm-store" node_modules
mkdir -p "$BUNDLE_DIR/pnpm-store"
pnpm config set store-dir "$BUNDLE_DIR/pnpm-store"
pnpm fetch --frozen-lockfile --force
pnpm install --frozen-lockfile --force

# Inject Linux SWC into portable tree
rm -rf node_modules/@next/swc-linux-x64-gnu node_modules/@next/swc-linux-x64-musl
mkdir -p node_modules/@next/swc-linux-x64-gnu node_modules/@next/swc-linux-x64-musl
tar -xzf "$SWC_GNU_TGZ" -C node_modules/@next/swc-linux-x64-gnu --strip-components=1
tar -xzf "$SWC_MUSL_TGZ" -C node_modules/@next/swc-linux-x64-musl --strip-components=1

# Validate frontend
if grep -q '"typecheck"' package.json; then pnpm typecheck; fi
if grep -q '"lint"' package.json; then pnpm lint; fi
if grep -q '"format:check"' package.json; then pnpm format:check; fi
rm -rf .next out
pnpm build

PORTABLE_NODE_MODULES="$BUNDLE_DIR/frontend-node-modules-linux-x64.tar.gz"
rm -f "$PORTABLE_NODE_MODULES"
COPYFILE_DISABLE=1 tar -czf "$PORTABLE_NODE_MODULES" -C "$FRONTEND_DIR" node_modules

# Verify pnpm offline store on host
rm -rf node_modules
pnpm install --offline --frozen-lockfile --force
rm -rf node_modules/@next/swc-linux-x64-gnu node_modules/@next/swc-linux-x64-musl
mkdir -p node_modules/@next/swc-linux-x64-gnu node_modules/@next/swc-linux-x64-musl
tar -xzf "$SWC_GNU_TGZ" -C node_modules/@next/swc-linux-x64-gnu --strip-components=1
tar -xzf "$SWC_MUSL_TGZ" -C node_modules/@next/swc-linux-x64-musl --strip-components=1
rm -rf .next out
pnpm build
rm -f "$PORTABLE_NODE_MODULES"
COPYFILE_DISABLE=1 tar -czf "$PORTABLE_NODE_MODULES" -C "$FRONTEND_DIR" node_modules

# Verify Maven offline repository
cd "$PROJECT_DIR"
"$MAVEN_HOME/bin/mvn" -o -Dmaven.repo.local="$MAVEN_LOCAL_REPO" -Dskip.frontend=true test
"$MAVEN_HOME/bin/mvn" -o -Dmaven.repo.local="$MAVEN_LOCAL_REPO" -Dskip.frontend=true -DskipTests package

# pnpm wrapper
cat > "$BUNDLE_DIR/bin/pnpm" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NODE_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'node-v*-linux-x64' | head -1)"
PNPM_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'pnpm-*' | head -1)"
exec "$NODE_HOME/bin/node" "$PNPM_HOME/bin/pnpm.cjs" "$@"
EOS
chmod +x "$BUNDLE_DIR/bin/pnpm"

# Maven wrapper
cat > "$BUNDLE_DIR/bin/mvn" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAVEN_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'apache-maven-*' | head -1)"
exec "$MAVEN_HOME/bin/mvn" "$@"
EOS
chmod +x "$BUNDLE_DIR/bin/mvn"

# Extract Linux system deps without root
cat > "$BUNDLE_DIR/prepare-linux-system-deps.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEB_DIR="$BUNDLE_DIR/linux-debs"
ROOT_DIR="$BUNDLE_DIR/linux-root"
command -v dpkg-deb >/dev/null 2>&1 || { echo "ERROR: dpkg-deb bulunamadı."; exit 1; }
rm -rf "$ROOT_DIR" && mkdir -p "$ROOT_DIR"
find "$DEB_DIR" -type f -name '*.deb' -print0 | while IFS= read -r -d '' DEB; do dpkg-deb -x "$DEB" "$ROOT_DIR"; done
EOS
chmod +x "$BUNDLE_DIR/prepare-linux-system-deps.sh"

# Linux environment
cat > "$BUNDLE_DIR/use-linux-runtime.sh" <<'EOS'
#!/usr/bin/env bash
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NODE_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'node-v*-linux-x64' | head -1)"
JAVA_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'graalvm-community-openjdk-*' | head -1)"
MAVEN_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'apache-maven-*' | head -1)"
PNPM_HOME="$(find "$BUNDLE_DIR/runtime" -maxdepth 1 -type d -name 'pnpm-*' | head -1)"
export BUNDLE_DIR NODE_HOME JAVA_HOME MAVEN_HOME PNPM_HOME
export MAVEN_REPO_LOCAL="$BUNDLE_DIR/maven-repository"
export PNPM_STORE_DIR="$BUNDLE_DIR/pnpm-store"
export LINUX_ROOT="$BUNDLE_DIR/linux-root"
export PATH="$LINUX_ROOT/usr/bin:$LINUX_ROOT/bin:$BUNDLE_DIR/bin:$JAVA_HOME/bin:$NODE_HOME/bin:$MAVEN_HOME/bin:$PATH"
export LD_LIBRARY_PATH="$LINUX_ROOT/usr/lib/x86_64-linux-gnu:$LINUX_ROOT/lib/x86_64-linux-gnu:$LINUX_ROOT/usr/lib:$LINUX_ROOT/lib:${LD_LIBRARY_PATH:-}"
export XKB_CONFIG_ROOT="$LINUX_ROOT/usr/share/X11/xkb"
EOS
chmod +x "$BUNDLE_DIR/use-linux-runtime.sh"

# Restore frontend
cat > "$BUNDLE_DIR/restore-frontend-linux.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${1:-$(pwd)}"
FRONTEND_DIR="$PROJECT_DIR/src/main/frontend"
ARCHIVE="$BUNDLE_DIR/frontend-node-modules-linux-x64.tar.gz"
[[ -f "$ARCHIVE" ]] || { echo "ERROR: node_modules archive bulunamadı."; exit 1; }
rm -rf "$FRONTEND_DIR/node_modules"
tar -xzf "$ARCHIVE" -C "$FRONTEND_DIR"
EOS
chmod +x "$BUNDLE_DIR/restore-frontend-linux.sh"

# Offline build
cat > "$BUNDLE_DIR/build-project-linux.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${1:-$(pwd)}"
FRONTEND_DIR="$PROJECT_DIR/src/main/frontend"
source "$BUNDLE_DIR/use-linux-runtime.sh"
"$BUNDLE_DIR/restore-frontend-linux.sh" "$PROJECT_DIR"
cd "$FRONTEND_DIR"
if grep -q '"typecheck"' package.json; then pnpm typecheck; fi
if grep -q '"lint"' package.json; then pnpm lint; fi
if grep -q '"format:check"' package.json; then pnpm format:check; fi
rm -rf .next out && pnpm build
cd "$PROJECT_DIR"
mvn -o -Dmaven.repo.local="$MAVEN_REPO_LOCAL" -Dskip.frontend=true test
mvn -o -Dmaven.repo.local="$MAVEN_REPO_LOCAL" -Dskip.frontend=true -DskipTests package
EOS
chmod +x "$BUNDLE_DIR/build-project-linux.sh"

# Dev run
cat > "$BUNDLE_DIR/run-dev-linux.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${1:-$(pwd)}"
source "$BUNDLE_DIR/use-linux-runtime.sh"
"$BUNDLE_DIR/build-project-linux.sh" "$PROJECT_DIR"
JAR="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1)"
[[ -n "$JAR" ]] || { echo "ERROR: executable JAR bulunamadı."; exit 1; }
exec java -jar "$JAR" --spring.profiles.active=dev
EOS
chmod +x "$BUNDLE_DIR/run-dev-linux.sh"

# Offline Cypress E2E
cat > "$BUNDLE_DIR/run-e2e-linux.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${1:-$(pwd)}"
FRONTEND_DIR="$PROJECT_DIR/src/main/frontend"
[[ -x "$BUNDLE_DIR/linux-root/usr/bin/Xvfb" ]] || "$BUNDLE_DIR/prepare-linux-system-deps.sh"
source "$BUNDLE_DIR/use-linux-runtime.sh"
"$BUNDLE_DIR/restore-frontend-linux.sh" "$PROJECT_DIR"
CYPRESS_BINARY="$(find "$BUNDLE_DIR/cypress" -type f -path '*/Cypress/Cypress' | head -1)"
[[ -n "$CYPRESS_BINARY" ]] || { echo "ERROR: Cypress binary bulunamadı."; exit 1; }
export CYPRESS_RUN_BINARY="$CYPRESS_BINARY"
export DISPLAY=:99
"$BUNDLE_DIR/linux-root/usr/bin/Xvfb" "$DISPLAY" -screen 0 1920x1080x24 -nolisten tcp >"$BUNDLE_DIR/xvfb.log" 2>&1 &
XVFB_PID=$!
trap 'kill "$XVFB_PID" >/dev/null 2>&1 || true' EXIT INT TERM
sleep 2
cd "$FRONTEND_DIR"
"$NODE_HOME/bin/node" "$FRONTEND_DIR/node_modules/cypress/bin/cypress" run --browser electron
EOS
chmod +x "$BUNDLE_DIR/run-e2e-linux.sh"

# Full build + dev backend + Cypress
cat > "$BUNDLE_DIR/run-all-linux.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${1:-$(pwd)}"
source "$BUNDLE_DIR/use-linux-runtime.sh"
"$BUNDLE_DIR/build-project-linux.sh" "$PROJECT_DIR"
JAR="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1)"
[[ -n "$JAR" ]] || { echo "ERROR: executable JAR bulunamadı."; exit 1; }
java -jar "$JAR" --spring.profiles.active=dev >"$BUNDLE_DIR/spring-boot-dev.log" 2>&1 &
APP_PID=$!
trap 'kill "$APP_PID" >/dev/null 2>&1 || true' EXIT INT TERM
READY=0
for _ in $(seq 1 90); do
  if curl -fsS http://127.0.0.1:9090/actuator/health/readiness >/dev/null 2>&1; then READY=1; break; fi
  kill -0 "$APP_PID" >/dev/null 2>&1 || { tail -100 "$BUNDLE_DIR/spring-boot-dev.log"; exit 1; }
  sleep 1
done
[[ "$READY" == 1 ]] || { echo "ERROR: readiness timeout"; tail -100 "$BUNDLE_DIR/spring-boot-dev.log"; exit 1; }
"$BUNDLE_DIR/run-e2e-linux.sh" "$PROJECT_DIR"
EOS
chmod +x "$BUNDLE_DIR/run-all-linux.sh"

# Manifest only; README is intentionally untouched/not generated
cd "$BUNDLE_DIR"
{
  echo "Generated: $(date)"
  echo "Node: $NODE_VERSION"
  echo "pnpm: $PNPM_VERSION"
  echo "Next: $NEXT_VERSION"
  echo "Cypress: $CYPRESS_VERSION"
  echo "Maven: $MAVEN_VERSION"
  echo "GraalVM: $GRAAL_VERSION"
  echo "Files:"
  find . -type f | sort
} > files.txt

mkdir -p "$DESKTOP"
rm -f "$OUTPUT"
cd "$BUNDLE_DIR"
COPYFILE_DISABLE=1 zip -r -q --symlinks "$OUTPUT" .
unzip -tq "$OUTPUT"
echo "DONE: $OUTPUT"
du -sh "$OUTPUT"
