package com.hims.pharmacy.api;

import com.hims.item.api.ItemMasterController;
import com.hims.item.application.ItemMasterService;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.pharmacy.configuration.JwtConfig;
import com.hims.pharmacy.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that in pharmacy-service bundle, absorbed module item's domain exception
 * is handled by com.hims.item.api.GlobalExceptionHandler (400) and NOT swallowed
 * into 500 by com.hims.pharmacy.api.GlobalExceptionHandler.
 */
@WebMvcTest(ItemMasterController.class)
@Import({
        ItemMasterController.class,
        SecurityConfig.class,
        JwtConfig.class,
        com.hims.pharmacy.api.GlobalExceptionHandler.class,
        com.hims.item.api.GlobalExceptionHandler.class
})
@AutoConfigureMockMvc
class ItemAdviceScopeWebMvcTest {

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
    void postItem_withInvalidData_returns400_not500() throws Exception {
        when(service.createItem(any(), any()))
                .thenThrow(new InvalidItemRequestException("BEXA003", "Item name is required"));

        mockMvc.perform(post("/api/v1/items")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").value("Item name is required"));
    }
}
