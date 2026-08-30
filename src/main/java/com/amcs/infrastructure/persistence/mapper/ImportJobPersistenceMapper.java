package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.persistence.entity.ImportJobEntity;
import com.amcs.infrastructure.persistence.entity.ImportJobErrorEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ImportJobPersistenceMapper {

    public ImportJobEntity toEntity(ImportJob domain) {
        if (domain == null) {
            return null;
        }

        ImportJobEntity entity = new ImportJobEntity(
            domain.getId(),
            domain.getImportType().name(),
            domain.getImportMode().name(),
            domain.getStatus().name(),
            domain.getOriginalFilename(),
            domain.getTotalRows(),
            domain.getValidRows(),
            domain.getInvalidRows(),
            domain.getCreatedByUserId(),
            domain.getCreatedAt(),
            domain.getCommittedAt(),
            domain.getDiscardedAt()
        );

        if (domain.getErrors() != null) {
            for (RowValidationError error : domain.getErrors()) {
                entity.addError(toErrorEntity(error, entity));
            }
        }

        return entity;
    }

    public ImportJob toDomain(ImportJobEntity entity, List<ImportJobErrorEntity> errorEntities) {
        if (entity == null) {
            return null;
        }

        List<RowValidationError> domainErrors = new ArrayList<>();
        if (errorEntities != null) {
            for (ImportJobErrorEntity errorEntity : errorEntities) {
                domainErrors.add(toErrorDomain(errorEntity));
            }
        }

        return ImportJob.reconstitute(
            entity.getId(),
            ImportType.valueOf(entity.getImportType()),
            ImportMode.valueOf(entity.getImportMode()),
            ImportStatus.valueOf(entity.getStatus()),
            entity.getOriginalFilename(),
            entity.getTotalRows(),
            entity.getValidRows(),
            entity.getInvalidRows(),
            domainErrors,
            entity.getCreatedByUserId(),
            entity.getCreatedAt(),
            entity.getCommittedAt(),
            entity.getDiscardedAt()
        );
    }

    public ImportJobErrorEntity toErrorEntity(RowValidationError domainError, ImportJobEntity jobEntity) {
        if (domainError == null) {
            return null;
        }

        return new ImportJobErrorEntity(
            UUID.randomUUID(),
            jobEntity,
            domainError.rowIndex(),
            domainError.columnName(),
            domainError.rejectedValue(),
            domainError.errorCode(),
            domainError.errorMessage()
        );
    }

    public RowValidationError toErrorDomain(ImportJobErrorEntity entity) {
        if (entity == null) {
            return null;
        }

        return new RowValidationError(
            entity.getRowIndex(),
            entity.getColumnName(),
            entity.getRejectedValue(),
            entity.getErrorCode(),
            entity.getErrorMessage()
        );
    }
}
