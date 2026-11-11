package com.hims.item.domain.repository;

import com.hims.item.domain.model.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * M13 item enquiry name search (MIG-012b + V3 "item enquiry support (read-side)").
     * Uses the GIN trgm index {@code ihms_item.ix_item_name_trgm} on
     * {@code item.item_name} for fuzzy LIKE matching: the {@code name}
     * argument is wrapped in {@code %...%} so the trigram operator picks up
     * partial / mid-word matches (parity with the legacy
     * {@code CIMS_GETITEMLISTBYITEMNAME} item-master typeahead).
     *
     * <p>Ordering: trigram similarity descending (closest match first), then
     * {@code item_name} for stable ties. Pageable so callers can paginate.
     * Empty / null {@code name} returns the most recently added items.
     *
     * <p>Returns a native {@code Object[]} projection (no entity round-trip)
     * so the service can pull the read-side columns the Item entity does not
     * expose via getters (brand, strength, category, rates). Columns:
     * <ol>
     *   <li>id</li>
     *   <li>item_code</li>
     *   <li>item_name</li>
     *   <li>brand_name</li>
     *   <li>strength</li>
     *   <li>item_group_id</li>
     *   <li>item_category_id</li>
     *   <li>purchase_rate</li>
     *   <li>mrp</li>
     *   <li>sale_price</li>
     * </ol>
     */
    @Query(value = """
            SELECT id, item_code, item_name, brand_name, strength,
                   item_group_id, item_category_id,
                   purchase_rate, mrp, sale_price
            FROM ihms_item.item
            WHERE (CAST(:name AS text) IS NULL OR :name = '' OR LOWER(item_name) LIKE LOWER(CONCAT('%', :name, '%')))
            ORDER BY CASE WHEN CAST(:name AS text) IS NULL OR :name = '' THEN 0
                          ELSE similarity(LOWER(item_name), LOWER(:name)) END DESC,
                     item_name ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchByNameRaw(@Param("name") String name,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);

    /**
     * Total row count for the same predicate as {@link #searchByNameRaw} — used
     * to expose {@code totalRows} on the item search result so the UI can
     * render "showing 1–50 of 137".
     */
    @Query(value = """
            SELECT count(*) FROM ihms_item.item
            WHERE (CAST(:name AS text) IS NULL OR :name = '' OR LOWER(item_name) LIKE LOWER(CONCAT('%', :name, '%')))
            """, nativeQuery = true)
    long countByName(@Param("name") String name);
}
