package com.amcs.domain.importer;

/**
 * Strategy for handling validation errors during bulk import.
 */
public enum ImportMode {
    /**
     * If any row fails validation, the entire import job is rejected and no rows are committed.
     */
    FAIL_FAST,

    /**
     * Valid rows are staged and can be confirmed for commit; invalid rows are reported and skipped.
     */
    PARTIAL_COMMIT
}
