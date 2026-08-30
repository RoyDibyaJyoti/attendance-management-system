package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.importer.ImportStagedRow;
import com.amcs.infrastructure.persistence.entity.ImportStagedRowEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataImportStagedRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportStagedRowPersistenceAdapter Unit Tests")
class ImportStagedRowPersistenceAdapterTest {

    @Mock
    private SpringDataImportStagedRowRepository repository;

    private ImportStagedRowPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ImportStagedRowPersistenceAdapter(repository);
    }

    @Test
    @DisplayName("saveAll maps domain staged rows to entities and persists them")
    void shouldSaveAllStagedRows() {
        UUID jobId = UUID.randomUUID();
        ImportStagedRow row1 = ImportStagedRow.create(jobId, 2, "STUDENTS", "{\"reg\":\"CS01\"}");
        ImportStagedRow row2 = ImportStagedRow.create(jobId, 3, "STUDENTS", "{\"reg\":\"CS02\"}");

        adapter.saveAll(List.of(row1, row2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportStagedRowEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ImportStagedRowEntity> entities = captor.getValue();
        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).getJobId()).isEqualTo(jobId);
        assertThat(entities.get(0).getRowIndex()).isEqualTo(2);
        assertThat(entities.get(0).getPayloadJson()).isEqualTo("{\"reg\":\"CS01\"}");
    }

    @Test
    @DisplayName("findByJobId retrieves staged rows ordered by row index")
    void shouldFindStagedRowsByJobId() {
        UUID jobId = UUID.randomUUID();
        ImportStagedRowEntity e1 = new ImportStagedRowEntity(UUID.randomUUID(), jobId, 2, "STUDENTS", "{\"reg\":\"CS01\"}");
        ImportStagedRowEntity e2 = new ImportStagedRowEntity(UUID.randomUUID(), jobId, 3, "STUDENTS", "{\"reg\":\"CS02\"}");

        when(repository.findByJobIdOrderByRowIndexAsc(jobId)).thenReturn(List.of(e1, e2));

        List<ImportStagedRow> rows = adapter.findByJobId(jobId);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowIndex()).isEqualTo(2);
        assertThat(rows.get(1).rowIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("deleteByJobId calls repository delete")
    void shouldDeleteByJobId() {
        UUID jobId = UUID.randomUUID();
        adapter.deleteByJobId(jobId);
        verify(repository).deleteByJobId(jobId);
    }
}
