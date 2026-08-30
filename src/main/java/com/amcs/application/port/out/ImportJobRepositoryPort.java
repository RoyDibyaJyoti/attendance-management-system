package com.amcs.application.port.out;

import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for managing {@link ImportJob} aggregate persistence and staging lifecycle.
 *
 * <p><strong>Pure Hexagonal Isolation:</strong> Independent of Spring Data, JPA, Hibernate, and SQL.
 */
public interface ImportJobRepositoryPort {

    /**
     * Persists or updates an import job along with its associated row validation errors.
     *
     * @param importJob the domain import job aggregate to persist
     * @return the persisted domain import job
     */
    ImportJob save(ImportJob importJob);

    /**
     * Retrieves an import job by its unique identifier, including all staged validation errors.
     *
     * @param id unique identifier of the import job
     * @return optional containing the reconstituted domain job if found
     */
    Optional<ImportJob> findById(UUID id);

    /**
     * Retrieves all import jobs submitted by a specific user.
     *
     * @param userId unique identifier of the creating user account
     * @return list of import jobs ordered by creation timestamp descending
     */
    List<ImportJob> findByCreatedByUserId(UUID userId);

    /**
     * Retrieves all import jobs matching a specific lifecycle status.
     *
     * @param status lifecycle status to filter by
     * @return list of matching import jobs
     */
    List<ImportJob> findByStatus(ImportStatus status);

    /**
     * Retrieves all import jobs matching a specific import type.
     *
     * @param type import type to filter by
     * @return list of matching import jobs
     */
    List<ImportJob> findByImportType(ImportType type);

    /**
     * Deletes an import job by its unique identifier, cascading deletion to all staged errors.
     *
     * @param id unique identifier of the import job to delete
     */
    void deleteById(UUID id);
}
