#!/usr/bin/env sh

if [ -x "$(command -v gradle)" ]; then
  exec gradle "$@"
fi

cat <<'EOF'
Gradle is required to run this project.
Install Gradle and rerun ./gradlew.
EOF
exit 1
