package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.StockTransfer;
import com.hims.item.domain.model.StockTransferDetail;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StockTransferDetailRepository;
import com.hims.item.domain.repository.StockTransferRepository;
import com.hims.item.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Inter-store transfer workflow; all quantity changes use the D-1 stock writer. */
@Service
public class StockTransferService {
    private static final long ACTOR = 1L;
    private final StockTransferRepository transfers; private final StockTransferDetailRepository details;
    private final StoreRepository stores; private final ItemRepository items; private final StockService stock;
    public StockTransferService(StockTransferRepository transfers, StockTransferDetailRepository details, StoreRepository stores, ItemRepository items, StockService stock) {
        this.transfers=transfers; this.details=details; this.stores=stores; this.items=items; this.stock=stock;
    }
    public record Line(Long itemId, Long batchId, BigDecimal quantity, String remarks) {}
    @Transactional public StockTransfer create(Long from, Long to, String remarks, List<Line> lines) {
        requireStore(from); requireStore(to);
        if (from.equals(to)) throw new InvalidItemRequestException("BEXA003", "Source and destination stores must differ");
        if (lines == null || lines.isEmpty()) throw new InvalidItemRequestException("BEXA003", "At least one transfer line is required");
        StockTransfer t=new StockTransfer(); t.setId(transfers.nextTransferId()); t.setTransferNo("TRF-"+t.getId()); t.setFromStoreId(from); t.setToStoreId(to); t.setRemarks(remarks); t.setDocStatus(StockTransfer.STATUS_REQUESTED); t.setStatus(true); t.setAddedBy(ACTOR); t.setAddedDatetime(Timestamp.from(Instant.now())); t.setSynchronizedd(false); transfers.save(t);
        for (Line line: lines) { if (line.itemId()==null || !items.existsById(line.itemId()) || line.quantity()==null || line.quantity().signum()<=0) throw new InvalidItemRequestException("BEXA003", "Each transfer line needs an existing item and quantity > 0"); StockTransferDetail d=new StockTransferDetail(); d.setId(details.nextDetailId()); d.setTransferId(t.getId()); d.setItemId(line.itemId()); d.setBatchId(line.batchId()); d.setQuantity(line.quantity()); d.setReceivedQty(BigDecimal.ZERO); d.setRemarks(line.remarks()); d.setStatus(true); d.setAddedBy(ACTOR); d.setAddedDatetime(Timestamp.from(Instant.now())); d.setSynchronizedd(false); details.save(d); }
        return t;
    }
    @Transactional public StockTransfer dispatch(long id, Long actor) { StockTransfer t=get(id); requireState(t,StockTransfer.STATUS_REQUESTED); for (StockTransferDetail d:details.findByTransferId(id)) stock.applyMovement(t.getFromStoreId(),d.getItemId(),d.getBatchId(),null,(short)7,StockService.OP_SUBTRACT,d.getQuantity(),"STOCK_TRANSFER",id,d.getRemarks()); t.setDocStatus(StockTransfer.STATUS_DISPATCHED); t.setDispatchedBy(actor==null?ACTOR:actor); t.setDispatchedAt(Timestamp.from(Instant.now())); stamp(t); return transfers.save(t); }
    @Transactional public StockTransfer receive(long id, Long actor) { StockTransfer t=get(id); requireState(t,StockTransfer.STATUS_DISPATCHED); for (StockTransferDetail d:details.findByTransferId(id)) { BigDecimal received=d.getReceivedQty()==null?BigDecimal.ZERO:d.getReceivedQty(); BigDecimal outstanding=d.getQuantity().subtract(received); if (outstanding.signum()>0) { stock.applyMovement(t.getToStoreId(),d.getItemId(),d.getBatchId(),null,(short)6,StockService.OP_ADD,outstanding,"STOCK_TRANSFER",id,d.getRemarks()); d.setReceivedQty(d.getQuantity()); details.save(d); } } t.setDocStatus(StockTransfer.STATUS_RECEIVED); t.setReceivedBy(actor==null?ACTOR:actor); t.setReceivedAt(Timestamp.from(Instant.now())); stamp(t); return transfers.save(t); }
    @Transactional(readOnly=true) public StockTransfer get(long id){return transfers.findById(id).orElseThrow(()->new ItemNotFoundException("NOT_FOUND","Stock transfer not found"));}
    @Transactional(readOnly=true) public List<StockTransfer> list(Long storeId,Short status){if(status!=null)return transfers.findByDocStatusOrderByAddedDatetimeDesc(status); if(storeId!=null)return transfers.findByFromStoreIdOrderByAddedDatetimeDesc(storeId); return transfers.findAll();}
    @Transactional(readOnly=true) public List<StockTransferDetail> details(long id){get(id);return details.findByTransferId(id);}
    private void requireStore(Long id){if(id==null||!stores.existsById(id))throw new ItemNotFoundException("NOT_FOUND","Store not found");}
    private void requireState(StockTransfer t,short state){if(t.getDocStatus()!=state)throw new InvalidItemRequestException("ITEM_STATE","Invalid transfer state");}
    private void stamp(StockTransfer t){t.setUpdatedBy(ACTOR);t.setUpdatedDatetime(Timestamp.from(Instant.now()));}
}
