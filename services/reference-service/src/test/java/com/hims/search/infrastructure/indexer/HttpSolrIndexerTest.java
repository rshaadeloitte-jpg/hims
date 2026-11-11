package com.hims.search.infrastructure.indexer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * L0 — HttpSolrIndexer wire contract via MockRestServiceServer bound to the
 * RestClient.Builder: update JSON shape ({add:[{doc:{id, <k>_s, text_t}}]}),
 * commit=true on writes, edismax select with the configured patient qf boosts,
 * response parsing (numFound + docs), and failure mapping to IndexerException.
 * The REAL round-trip against a Testcontainers solr:9 core runs at L2.
 */
class HttpSolrIndexerTest {

    private MockRestServiceServer server;
    private HttpSolrIndexer indexer;

    private static final String PATIENT_QF = "text_t^1 mrno_s^18 mobile_s^16 name_s^8";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        indexer = new HttpSolrIndexer(builder.build(), PATIENT_QF);
    }

    @Test
    void index_postsAddWithCommitAndDynamicFields() {
        server.expect(requestTo("/solr/patient/update?commit=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.add[0].doc.id").value("P-1"))
                .andExpect(jsonPath("$.add[0].doc.mrno_s").value("MRN-77"))
                .andExpect(jsonPath("$.add[0].doc.text_t").value("MRN-77 Aditya"))
                .andRespond(withSuccess("{\"responseHeader\":{}}", MediaType.APPLICATION_JSON));

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("mrno", "MRN-77");
        payload.put("name", "Aditya");
        indexer.index("patient", "P-1", payload);
        server.verify();
    }

    @Test
    void delete_postsDeleteByIdWithCommit() {
        server.expect(requestTo("/solr/encounter/update?commit=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.delete.id").value("E-5"))
                .andRespond(withSuccess("{\"responseHeader\":{}}", MediaType.APPLICATION_JSON));

        indexer.delete("encounter", "E-5");
        server.verify();
    }

    @Test
    void search_usesEdismax_withPatientQfBoosts_andParsesNumFoundPlusDocs() {
        server.expect(requestTo(startsWith("/solr/patient/select?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("defType", "edismax"))
                .andExpect(queryParam("q", "aditya"))
                .andExpect(queryParam("qf", PATIENT_QF.replace(" ", "%20").replace("^", "%5E")))
                .andExpect(queryParam("rows", "10"))
                .andExpect(queryParam("start", "0"))
                .andExpect(queryParam("wt", "json"))
                .andRespond(withSuccess("""
                        {"response":{"numFound":2,"docs":[
                          {"id":"P-1","mrno_s":["MRN-77"]},
                          {"id":"P-2","name_s":["Other Patient"]}]}
                        }""", MediaType.APPLICATION_JSON));

        SearchIndexer.SearchResultPage page = indexer.search("patient", "aditya", 10, 0);
        server.verify();
        assertThat(page.numFound()).isEqualTo(2);
        assertThat(page.docs()).hasSize(2);
        assertThat(page.docs().get(0)).containsKey("id");
    }

    @Test
    void search_nonPatientCollection_usesDefaultTextBoost_only() {
        server.expect(requestTo(startsWith("/solr/icd10cm/select?")))
                .andExpect(queryParam("qf", "text_t%5E1"))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0,\"docs\":[]}}",
                        MediaType.APPLICATION_JSON));

        assertThat(indexer.search("icd10cm", "fever", 10, 0).numFound()).isZero();
        server.verify();
    }

    @Test
    void backendError_mapsToIndexerException_forCallerJournaling() {
        server.expect(requestTo("/solr/item/update?commit=true"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> indexer.index("item", "I-1", Map.of("code", "C")))
                .isInstanceOf(SearchIndexer.IndexerException.class)
                .hasMessageContaining("Solr update failed");
        server.verify();
    }

    @Test
    void nullPayloadValues_areSkippedInSolrDoc() {
        server.expect(requestTo("/solr/patient/update?commit=true"))
                .andExpect(jsonPath("$.add[0].doc.text_t").value("v1"))
                .andExpect(jsonPath("$.add[0].doc.a_s").value("v1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("a", "v1");
        payload.put("b", null);
        indexer.index("patient", "P-2", payload);
        server.verify();
    }
}
