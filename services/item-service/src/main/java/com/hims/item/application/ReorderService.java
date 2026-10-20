package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.ItemReorderPolicy;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.repository.ItemReorderPolicyRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.ItemStockBalanceRepository;
import com.hims.item.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** M13 reorder/safety policy and current low-stock read model. */
@Service
public class ReorderService {
    private static final long ACTOR=1L;
    private final ItemReorderPolicyRepository policies; private final ItemRepository items; private final StoreRepository stores; private final ItemStockBalanceRepository balances; private final ObjectMapper mapper;
    public ReorderService(ItemReorderPolicyRepository policies, ItemRepository items, StoreRepository stores, ItemStockBalanceRepository balances, ObjectMapper mapper){this.policies=policies;this.items=items;this.stores=stores;this.balances=balances;this.mapper=mapper;}
    @Transactional public ItemReorderPolicy upsert(JsonNode body){
        if(body==null) throw new InvalidItemRequestException("BEXA003","Invalid input data !"); Long store=id(body,"storeId"), item=id(body,"itemId");
        if(store==null||!stores.existsById(store)) throw new ItemNotFoundException("NOT_FOUND","Store not found"); if(item==null||!items.existsById(item)) throw new ItemNotFoundException("NOT_FOUND","Item not found");
        BigDecimal level=decimal(body,"reorderLevel"), value=decimal(body,"reorderValue"), safety=decimal(body,"safetyStock"); if(level==null&&value==null) throw new InvalidItemRequestException("BEXA003","reorderLevel or reorderValue is required");
        ItemReorderPolicy p=policies.findByStoreIdAndItemId(store,item).orElseGet(()->{ItemReorderPolicy x=new ItemReorderPolicy();x.setId(policies.nextPolicyId());x.setStoreId(store);x.setItemId(item);x.setAddedBy(ACTOR);x.setAddedDatetime(Timestamp.from(Instant.now()));x.setStatus(true);x.setSynchronizedd(false);return x;});
        if(level!=null)p.setReorderLevel(level);if(value!=null)p.setReorderValue(value);if(safety!=null)p.setSafetyStock(safety);if(body.has("isAutomatic"))p.setIsAutomatic(body.get("isAutomatic").asBoolean());p.setUpdatedBy(ACTOR);p.setUpdatedDatetime(Timestamp.from(Instant.now()));return policies.save(p);
    }
    @Transactional(readOnly=true) public List<ItemReorderPolicy> policies(Long store){return store==null?policies.findAll():policies.findByStoreId(store);}
    public record ReorderAlert(ItemReorderPolicy policy, BigDecimal availableQty, BigDecimal suggestedQty){ }
    @Transactional(readOnly=true) public List<ReorderAlert> alerts(Long store){List<ReorderAlert> out=new ArrayList<>();for(ItemReorderPolicy p:policies(store)){if(store!=null&&!store.equals(p.getStoreId()))continue;BigDecimal available=balances.findByStoreIdAndItemId(p.getStoreId(),p.getItemId()).stream().map(ItemStockBalance::getAvailableQty).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal threshold=p.getReorderLevel()!=null?p.getReorderLevel():p.getReorderValue();if(threshold!=null&&available.compareTo(threshold)<=0){BigDecimal target=p.getReorderValue()!=null?p.getReorderValue():threshold;out.add(new ReorderAlert(p,available,target.subtract(available).max(BigDecimal.ZERO)));}}return out;}
    private static Long id(JsonNode n,String f){JsonNode v=n.path(f);return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong();} private static BigDecimal decimal(JsonNode n,String f){JsonNode v=n.path(f);return v.isMissingNode()||v.isNull()?null:v.decimalValue();}
}
