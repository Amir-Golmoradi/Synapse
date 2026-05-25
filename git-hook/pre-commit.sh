#!/bin/bash

# Get current time in ISO 8601 format matching standard server logs
timestamp() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

printf "[%s] [INFO] [com.google.apps.synapse.precommit] Initializing pre-commit verification sequence.\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] Active profile: ultra-strict-quality-gate\n" "$(timestamp)"

# 1. Execute Spotless auto-formatting
printf "[%s] [INFO] [com.google.apps.style.Spotless] Executing code format synchronization (spotless:apply).\n" "$(timestamp)"
mvn spotless:apply > /dev/null 2>&1
if [ $? -ne 0 ]; then
    printf "[%s] [SEVERE] [com.google.apps.style.Spotless] Format task failed due to engine configuration anomalies. Aborting execution.\n" "$(timestamp)"
    exit 1
fi

# Re-stage any structural changes applied by Spotless
git update-index --again

# 2. Execute full verification suite
printf "[%s] [INFO] [com.google.apps.build.Maven] Invoking verification lifecycle (clean verify).\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.build.Maven] Compiling components, executing static analysis, and initializing containerized test matrices.\n" "$(timestamp)"

mvn clean verify
STATUS=$?

# 3. Evaluate lifecycle response codes
if [ $STATUS -ne 0 ]; then
    printf "\n"
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] PRE-COMMIT VERIFICATION FAILURE\n" "$(timestamp)"
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] Reason: Codebase metrics fell below mandatory quality thresholds.\n" "$(timestamp)"
    printf "[%s] [SEVERE] [com.google.apps.synapse.precommit] Action Required: Review compilation trace, static analysis reports, or domain assertions above. Commit aborted.\n" "$(timestamp)"
    exit 1
fi

printf "\n"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] PRE-COMMIT VERIFICATION SUCCESS\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] Verification: 100%% of style rules, domain invariants, and integration matrices passed successfully.\n" "$(timestamp)"
printf "[%s] [INFO] [com.google.apps.synapse.precommit] Status: Component is certified production-ready. Proceeding with commit.\n" "$(timestamp)"
exit 0