package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.ItemMasterService;
import com.hims.item.domain.model.Item;
import com.hims.item.domain.model.ItemBatch;
import com.hims.item.domain.model.Store;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Item/Store/Batch masters (MIG-012 first slice). Server-owned ids (per-table
 * sequences) + audit fields; every write emits MasterDataChanged via outbox.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Item Masters (MIG-012)", description = "Item / Store / Batch master CRUD + MasterDataChanged outbox")
public class ItemMasterController {

    private final ItemMasterService service;
    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final ItemBatchRepository batchRepository;

    public ItemMasterController(ItemMasterService service,
                                ItemRepository itemRepository,
                                StoreRepository storeRepository,
                                ItemBatchRepository batchRepository) {
        this.service = service;
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
        this.batchRepository = batchRepository;
    }

    // ---- items ----
    @PostMapping("/items")
    public ResponseEntity<Item> createItem(@RequestBody(required = false) JsonNode body,
                                           @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createItem(body, unitId));
    }

    @PutMapping("/items/{id}")
    public Item updateItem(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateItem(id, body);
    }

    @DeleteMapping("/items/{id}")
    public Item removeItem(@PathVariable long id) {
        return service.removeItem(id);
    }

    @GetMapping("/items")
    public List<Item> listItems() {
        return itemRepository.findAll();
    }

    @GetMapping("/items/{id}")
    public Item getItem(@PathVariable long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new com.hims.item.application.exceptions.ItemNotFoundException("NOT_FOUND", "Record not found"));
    }

    // ---- stores ----
    @PostMapping("/stores")
    public ResponseEntity<Store> createStore(@RequestBody(required = false) JsonNode body,
                                             @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createStore(body, unitId));
    }

    @PutMapping("/stores/{id}")
    public Store updateStore(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateStore(id, body);
    }

    @DeleteMapping("/stores/{id}")
    public Store removeStore(@PathVariable long id) {
        return service.removeStore(id);
    }

    @GetMapping("/stores")
    public List<Store> listStores() {
        return storeRepository.findAll();
    }

    @GetMapping("/stores/{id}")
    public Store getStore(@PathVariable long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new com.hims.item.application.exceptions.ItemNotFoundException("NOT_FOUND", "Record not found"));
    }

    // ---- batches ----
    @PostMapping("/batches")
    public ResponseEntity<ItemBatch> createBatch(@RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBatch(body, unitId));
    }

    @PutMapping("/batches/{id}")
    public ItemBatch updateBatch(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateBatch(id, body);
    }

    @DeleteMapping("/batches/{id}")
    public ItemBatch removeBatch(@PathVariable long id) {
        return service.removeBatch(id);
    }

    @GetMapping("/batches")
    public List<ItemBatch> listBatches() {
        return batchRepository.findAll();
    }

    @GetMapping("/batches/{id}")
    public ItemBatch getBatch(@PathVariable long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new com.hims.item.application.exceptions.ItemNotFoundException("NOT_FOUND", "Record not found"));
    }
}
