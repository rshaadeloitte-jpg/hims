package com.hims.item.application;

import java.math.BigDecimal;

public final class ItemEnquiryView {

    private ItemEnquiryView() {}

    public record SearchRow(
            Long itemId,
            String itemCode,
            String itemName,
            String brandName,
            String strength,
            Long itemGroupId,
            Long itemCategoryId,
            BigDecimal purchaseRate,
            BigDecimal mrp,
            BigDecimal salePrice,
            Long storeId,
            BigDecimal availableQty,
            Long totalRows) {}

    public record SupplierLink(
            Long itemSupplierId,
            Long supplierId,
            String supplierCode,
            String supplierName,
            Boolean isManufacturer,
            Boolean isInhouseSupplier,
            Boolean isEmpanelled,
            Long itemCategoryId) {}
}
