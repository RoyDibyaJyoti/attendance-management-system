package com.amcs.application.port.out.importer;

import java.util.List;
import java.util.UUID;

/**
 * Outbound repository port for managing staged import row persistence.
 */
public interface ImportStagedRowRepositoryPort {

    /**
     * Persists a batch of staged rows.
     */
    void saveAll(List<ImportStagedRow> stagedRows);

    /**
     * Retrieves all staged rows for an import job ordered by row index ascending.
     */
    List<ImportStagedRow> findByJobId(UUID jobId);

    /**
     * Deletes all staged rows associated with an import job.
     */
    void deleteByJobId(UUID jobId);

    /**
     * Returns the count of staged rows for a job.
     */
    long countByJobId(UUID jobId);
}
