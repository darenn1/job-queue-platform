set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT/job-queue-platform-raw"

echo "=== Phase 1 Smoke Test ==="
echo "Project root: $PROJECT_ROOT"

echo ""
echo "--- Building project (mvn compile test-compile) ---"
mvn -q clean compile test-compile

echo ""
echo "--- Running StressTest (200 jobs, 4 workers, -verbose:gc) ---"
CLASSPATH="target/classes"

set +e 
java -verbose:gc -cp "$CLASSPATH" com.jobqueue.stress.StressTest
STRESS_TEST_EXIT_CODE=$?
set -e

echo ""
if [ "$STRESS_TEST_EXIT_CODE" -eq 0 ]; then
    echo "=== Phase 1 Smoke Test: PASSED ==="
else
    echo "=== Phase 1 Smoke Test: FAILED (exit code $STRESS_TEST_EXIT_CODE) ==="
fi

exit "$STRESS_TEST_EXIT_CODE"