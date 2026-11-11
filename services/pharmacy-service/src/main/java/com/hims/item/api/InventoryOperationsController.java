package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.ExpiryReturnService;
import com.hims.item.application.StockTransferService;
import com.hims.item.application.ReorderService;
import com.hims.item.domain.model.ItemReorderPolicy;
import com.hims.item.domain.model.ExpiryReturn;
import com.hims.item.domain.model.ExpiryReturnDetail;
import com.hims.item.domain.model.StockTransfer;
import com.hims.item.domain.model.StockTransferDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryOperationsController {
    private final ExpiryReturnService expiry; private final StockTransferService transfer; private final ReorderService reorder; private final com.hims.item.application.StockService stock;
    public InventoryOperationsController(ExpiryReturnService expiry, StockTransferService transfer, ReorderService reorder, com.hims.item.application.StockService stock){this.expiry=expiry;this.transfer=transfer;this.reorder=reorder;this.stock=stock;}
    @PostMapping("/expiry-returns") public ResponseEntity<ExpiryReturn> createReturn(@RequestBody JsonNode b){List<ExpiryReturnService.ReturnLine> ls=new ArrayList<>();for(JsonNode l:b.path("lines"))ls.add(new ExpiryReturnService.ReturnLine(longVal(l,"itemId"),longVal(l,"batchId"),longVal(l,"grnId"),decimal(l,"quantity"),decimal(l,"purchaseRate"),text(l,"returnReason")));return ResponseEntity.status(HttpStatus.CREATED).body(expiry.create(longVal(b,"storeId"),longVal(b,"supplierId"),shortVal(b,"returnType"),text(b,"remarks"),ls));}
    @GetMapping("/expiry-returns") public List<ExpiryReturn> returns(@RequestParam(required=false) Long storeId,@RequestParam(required=false) Short status){return expiry.list(storeId,status);}
    @GetMapping("/expiry-returns/{id}") public ExpiryReturn getReturn(@PathVariable long id){return expiry.get(id);}
    @GetMapping("/expiry-returns/{id}/lines") public List<ExpiryReturnDetail> returnLines(@PathVariable long id){return expiry.details(id);}
    @PostMapping("/expiry-returns/{id}/approve") public ExpiryReturn approve(@PathVariable long id){return expiry.approve(id,null);}
    @PostMapping("/expiry-returns/{id}/reject") public ExpiryReturn reject(@PathVariable long id){return expiry.reject(id);}
    @PostMapping("/expiry-returns/{id}/cancel") public ExpiryReturn cancel(@PathVariable long id){return expiry.cancel(id);}
    @PostMapping("/expiry-returns/{id}/return-to-supplier") public ExpiryReturn supplier(@PathVariable long id,@RequestBody JsonNode b){return expiry.returnToSupplier(id,text(b,"creditNoteNo"),decimal(b,"creditNoteAmount"));}
    @PostMapping("/opening-balances")
    public List<com.hims.item.domain.model.ItemStockBalance> openingBalances(@RequestBody JsonNode b){
        List<com.hims.item.domain.model.ItemStockBalance> out=new ArrayList<>();
        for(JsonNode l:b.path("lines")) out.add(stock.postMovement(longVal(l,"storeId"),longVal(l,"itemId"),longVal(l,"batchId"),null,(short)1,com.hims.item.application.StockService.OP_ADD,decimal(l,"quantity"),"OPENING_BALANCE",null,text(l,"remarks")));
        return out;
    }
    @PutMapping("/reorder-policies") public ItemReorderPolicy upsertPolicy(@RequestBody JsonNode b){return reorder.upsert(b);}
    @GetMapping("/reorder-policies") public List<ItemReorderPolicy> policies(@RequestParam(required=false) Long storeId){return reorder.policies(storeId);}
    @GetMapping("/reorder-alerts") public List<ReorderService.ReorderAlert> alerts(@RequestParam(required=false) Long storeId){return reorder.alerts(storeId);}
    @PostMapping("/transfers") public ResponseEntity<StockTransfer> createTransfer(@RequestBody JsonNode b){List<StockTransferService.Line> ls=new ArrayList<>();for(JsonNode l:b.path("lines"))ls.add(new StockTransferService.Line(longVal(l,"itemId"),longVal(l,"batchId"),decimal(l,"quantity"),text(l,"remarks")));return ResponseEntity.status(HttpStatus.CREATED).body(transfer.create(longVal(b,"fromStoreId"),longVal(b,"toStoreId"),text(b,"remarks"),ls));}
    @GetMapping("/transfers") public List<StockTransfer> transfers(@RequestParam(required=false) Long storeId,@RequestParam(required=false) Short status){return transfer.list(storeId,status);}
    @GetMapping("/transfers/{id}") public StockTransfer getTransfer(@PathVariable long id){return transfer.get(id);}
    @GetMapping("/transfers/{id}/lines") public List<StockTransferDetail> transferLines(@PathVariable long id){return transfer.details(id);}
    @PostMapping("/transfers/{id}/dispatch") public StockTransfer dispatch(@PathVariable long id){return transfer.dispatch(id,null);}
    @PostMapping("/transfers/{id}/receive") public StockTransfer receive(@PathVariable long id){return transfer.receive(id,null);}
    private static String text(JsonNode n,String f){JsonNode v=n.path(f);return v.isMissingNode()||v.isNull()?null:v.asText();}
    private static Long longVal(JsonNode n,String f){JsonNode v=n.path(f);return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong();}
    private static Short shortVal(JsonNode n,String f){JsonNode v=n.path(f);return v.isMissingNode()||v.isNull()?null:v.shortValue();}
    private static BigDecimal decimal(JsonNode n,String f){JsonNode v=n.path(f);return v.isMissingNode()||v.isNull()?null:v.decimalValue();}
}
