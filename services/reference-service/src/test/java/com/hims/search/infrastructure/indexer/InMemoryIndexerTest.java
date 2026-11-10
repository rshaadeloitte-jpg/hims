package com.hims.search.infrastructure.indexer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L0 — deterministic in-memory indexer fallback semantics: upsert by docId,
 * case-insensitive substring match over any value, deterministic ordering,
 * rows/start pagination, delete. Round-trip property (index then query returns
 * doc) is additionally pinned at L2 against real PG16 + the Solr container.
 */
class InMemoryIndexerTest {

    private final InMemoryIndexer indexer = new InMemoryIndexer();

    @Test
    void indexThenQuery_returnsDoc_roundTrip() {
        indexer.index("patient", "P-1", Map.of("mrno", "MRN-77", "name", "Aditya"));
        var page = indexer.search("patient", "aditya", 10, 0);
        assertThat(page.numFound()).isEqualTo(1);
        assertThat(page.docs().get(0)).containsEntry("id", "P-1");
        assertThat(page.docs().get(0)).containsEntry("mrno", "MRN-77");
    }

    @Test
    void match_isCaseInsensitiveSubstring_overAnyValue() {
        indexer.index("item", "I-1", Map.of("code", "PARA-500", "name", "Paracetamol"));
        assertThat(indexer.search("item", "etam", 10, 0).numFound()).isEqualTo(1);
        assertThat(indexer.search("item", "500", 10, 0).numFound()).isEqualTo(1);
        assertThat(indexer.search("item", "ibuprofen", 10, 0).numFound()).isZero();
    }

    @Test
    void pagination_rowsStart_deterministicOrder() {
        for (String id : List.of("D-3", "D-1", "D-2")) {
            indexer.index("icd10cm", id, Map.of("code", id));
        }
        var page = indexer.search("icd10cm", "D-", 2, 0);
        assertThat(page.numFound()).isEqualTo(3);
        assertThat(page.docs()).extracting(d -> d.get("id")).containsExactly("D-1", "D-2");

        var page2 = indexer.search("icd10cm", "D-", 2, 2);
        assertThat(page2.docs()).extracting(d -> d.get("id")).containsExactly("D-3");
    }

    @Test
    void upsertByDocId_replacesPriorDocument_nullValuesDropped() {
        java.util.Map<String, Object> withNull = new java.util.LinkedHashMap<>();
        withNull.put("code", "HF-1");
        withNull.put("note", null);
        indexer.index("healthfactor", "H-1", withNull);
        indexer.index("healthfactor", "H-1", Map.of("code", "HF-1b", "desc", "updated"));
        var page = indexer.search("healthfactor", "updated", 10, 0);
        assertThat(page.numFound()).isEqualTo(1);
        assertThat(page.docs().get(0)).doesNotContainKey("note").containsEntry("code", "HF-1b");
    }

    @Test
    void delete_removesDocument_andUnknownCollectionSearchIsEmpty() {
        indexer.index("encounter", "E-1", Map.of("opdNo", "OPD-9"));
        assertThat(indexer.docCount("encounter")).isEqualTo(1);
        indexer.delete("encounter", "E-1");
        assertThat(indexer.search("encounter", "OPD", 10, 0).numFound()).isZero();
        assertThat(indexer.docCount("encounter")).isZero();
        assertThat(indexer.search("unknown-collection", "x", 10, 0).numFound()).isZero();
    }

    @Test
    void guards_blankCollectionOrQ_rejected() {
        assertThatThrownBy(() -> indexer.index("", "X", Map.of()))
                .isInstanceOf(SearchIndexer.IndexerException.class);
        assertThatThrownBy(() -> indexer.search("patient", "  ", 10, 0))
                .isInstanceOf(SearchIndexer.IndexerException.class);
    }
}
