#!/usr/bin/env bash
set -e

# Change to the project root directory
cd "$(dirname "$0")/.."

# Extract the version from pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Extracted version from pom.xml: $VERSION"

DOC_FILES="README.md docs/07_DEPLOY_K8S.md docs/03_DEVELOPER_GUIDE.md docs/02_USER_GUIDE.md deploy/k8s/flink-operator-deployment.yaml"

for file in $DOC_FILES; do
  if [ -f "$file" ]; then
    echo "Updating $file..."
    # Use python to do regex replacement to avoid sed portability issues between macOS and Linux
    python3 -c "
import re, sys
with open(sys.argv[1], 'r') as f:
    content = f.read()

# Update JAR filename reference
content = re.sub(r'flinkflow-([0-9.]+[-A-Za-z0-9]*|\{version\})\.jar', f'flinkflow-{sys.argv[2]}.jar', content)

# Update Docker pull command reference
content = re.sub(r'ghcr\.io/talwegai/flinkflow:[a-zA-Z0-9.-]*(\{?version\}?)?', f'ghcr.io/talwegai/flinkflow:{sys.argv[2]}', content)

with open(sys.argv[1], 'w') as f:
    f.write(content)
" "$file" "$VERSION"
  else
    echo "Warning: $file not found."
  fi
done

echo "Synchronization complete!"
