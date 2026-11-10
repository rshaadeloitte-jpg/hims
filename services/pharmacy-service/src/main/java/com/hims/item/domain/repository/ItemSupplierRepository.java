package com.hims.item.domain.repository;

import com.hims.item.application.ItemEnquiryView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only projection over {@code ihms_item.item_supplier} →
 * {@code ihms_item.supplier}. The M13 item-enquiry suppliers list is a pure
 * projection (no entity, no writes), so this repo executes a single native
 * SQL via {@link EntityManager} and maps the {@code Object[]} rows into
 * {@link ItemEnquiryView.SupplierLink} records. Implemented as a plain
 * {@code @Repository} (not a Spring Data {@code JpaRepository}) because there
 * is no JPA entity for the link table; the read query does the join directly.
 */
@Repository
public class ItemSupplierRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Legacy parity: T_ITEMSUPPLIER join M_SUPPLIER. Returns one row per
     * supplier link for the given item, ordered by supplier code ascending.
     */
    @SuppressWarnings("unchecked")
    public List<ItemEnquiryView.SupplierLink> findSuppliersForItem(Long itemId) {
        Query q = entityManager.createNativeQuery("""
                SELECT isp.id, s.id, s.code, s.description,
                       s.is_manufacturer, s.is_inhouse_supplier, s.is_empanelled,
                       isp.item_category_id
                FROM ihms_item.item_supplier isp
                JOIN ihms_item.supplier s ON s.id = isp.supplier_id
                WHERE isp.item_id = :itemId
                ORDER BY s.code ASC
                """);
        q.setParameter("itemId", itemId);
        List<Object[]> rows = q.getResultList();
        List<ItemEnquiryView.SupplierLink> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new ItemEnquiryView.SupplierLink(
                    asLong(r[0]),
                    asLong(r[1]),
                    r[2] == null ? null : r[2].toString(),
                    r[3] == null ? null : r[3].toString(),
                    asBoolean(r[4]),
                    asBoolean(r[5]),
                    asBoolean(r[6]),
                    asLong(r[7])));
        }
        return out;
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static Boolean asBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return Boolean.valueOf(o.toString());
    }
}
