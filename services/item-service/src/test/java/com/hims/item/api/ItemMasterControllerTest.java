package com.hims.item.api;

import com.hims.item.application.ItemMasterService;
import com.hims.item.configuration.SecurityConfig;
import com.hims.item.domain.model.Item;
import com.hims.item.domain.model.ItemBatch;
import com.hims.item.domain.model.Store;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the item/store/batch master API (default-deny enforced). Service + repositories mocked. */
@WebMvcTest(ItemMasterController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ItemMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemMasterService service;

    @MockitoBean
    private ItemRepository itemRepository;

    @MockitoBean
    private StoreRepository storeRepository;

    @MockitoBean
    private ItemBatchRepository batchRepository;

    @Test
    void createItem_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createItem_returns201() throws Exception {
        Item i = new Item();
        i.setId(5L);
        i.setItemName("Ceftriaxone 1g");
        when(service.createItem(any(), isNull())).thenReturn(i);

        mockMvc.perform(post("/api/v1/items")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Ceftriaxone 1g\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.itemName").value("Ceftriaxone 1g"));
    }

    @Test
    void listItems_delegatesToRepository() throws Exception {
        Item i = new Item();
        i.setId(5L);
        i.setItemName("Ceftriaxone 1g");
        when(itemRepository.findAll()).thenReturn(List.of(i));

        mockMvc.perform(get("/api/v1/items")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("Ceftriaxone 1g"));
    }

    @Test
    void getItem_unknownId_returns404Envelope() throws Exception {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/items/99")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void getStore_byId_returnsStore() throws Exception {
        Store s = new Store();
        s.setId(1L);
        s.setCode("MAIN");
        s.setDescription("Main store");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/v1/stores/1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MAIN"));
    }

    @Test
    void createBatch_returns201WithBatchCode() throws Exception {
        ItemBatch b = new ItemBatch();
        b.setId(7L);
        b.setItemId(20L);
        b.setBatchCode("B2026A");
        when(service.createBatch(any(), isNull())).thenReturn(b);

        mockMvc.perform(post("/api/v1/batches")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":20,\"batchCode\":\"B2026A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchCode").value("B2026A"));
    }
}
