package com.amcs.domain.importer;

/**
 * Lifecycle states of an {@link ImportJob}.
 */
public enum ImportStatus {
    /**
     * File has been uploaded and registered; awaiting parsing and validation.
     */
    SUBMITTED,

    /**
     * Parsing and validation completed with 0 errors; ready for confirmation and commit.
     */
    STAGED_CLEAN,

    /**
     * Parsing completed with some row validation errors under PARTIAL_COMMIT mode; ready for confirmation.
     */
    STAGED_PARTIAL,

    /**
     * Import failed due to unparseable file, zero valid rows, or validation errors under FAIL_FAST mode.
     */
    REJECTED,

    /**
     * Staged valid rows have been atomically committed into domain storage.
     */
    COMMITTED,

    /**
     * Staged job was discarded/cancelled by user without committing.
     */
    DISCARDED
}
