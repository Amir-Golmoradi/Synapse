#!/bin/bash

timestamp() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

printf "[%s] [INFO] [com.google.apps.synapse.precommit] Initializing pre-commit verification sequence.\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] Active profile: local-quality-gate\n" "$(timestamp)"

printf "[%s] [INFO] [com.google.apps.style.Spotless] Executing code format synchronization (spotless:apply).\n" "$(timestamp)"
./mvnw \
    --batch-mode \
    --no-transfer-progress \
    com.diffplug.spotless:spotless-maven-plugin:2.43.0:apply
if [ $? -ne 0 ]; then
    printf "[%s] [SEVERE] [com.google.apps.style.Spotless] Format task failed due to engine configuration anomalies. Aborting execution.\n" "$(timestamp)"
    exit 1
fi

if ! git update-index --again; then
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] Failed to restage Spotless changes. Aborting execution.\n" "$(timestamp)"
    exit 1
fi

printf "[%s] [INFO] [com.google.apps.build.Maven] Invoking verification lifecycle (clean verify).\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.build.Maven] Compiling components, running tests, and executing static analysis.\n" "$(timestamp)"

./mvnw --batch-mode --no-transfer-progress clean verify
STATUS=$?

if [ $STATUS -ne 0 ]; then
    printf "\n"
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] PRE-COMMIT VERIFICATION FAILURE\n" "$(timestamp)"
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] Reason: Codebase metrics fell below mandatory quality thresholds.\n" "$(timestamp)"
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] Action Required: Review compilation trace, static analysis reports, or domain assertions above. Commit aborted.\n" "$(timestamp)"
    exit 1
fi

printf "\n"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] PRE-COMMIT VERIFICATION SUCCESS\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] Verification: formatting, tests, and static analysis passed successfully.\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] Status: Proceeding with commit.\n" "$(timestamp)"
exit 0
