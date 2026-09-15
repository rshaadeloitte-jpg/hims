import { useState, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { itemEnquiryFilterSchema, type ItemEnquiryFilterValues } from "./zodSchema";
import { useItemEnquiry, type ItemEnquiryResult } from "./queries";

export function ItemEnquiry() {
  const { register, handleSubmit, watch } = useForm<ItemEnquiryFilterValues>({
    resolver: zodResolver(itemEnquiryFilterSchema),
    defaultValues: {
      keyword: "",
      storeId: undefined,
      category: "",
      lowStockOnly: false,
      status: "",
    },
  });

  const filters = watch();
  const { data: items = [], isLoading, isFetching } = useItemEnquiry({
    keyword: filters.keyword || undefined,
    storeId: filters.storeId || undefined,
    category: filters.category || undefined,
    lowStockOnly: filters.lowStockOnly || undefined,
    status: filters.status || undefined,
  });

  const categories = useMemo(
    () => Array.from(new Set(items.map((i) => i.category).filter(Boolean))) as string[],
    [items],
  );

  return (
    <div className="item-enquiry">
      <form onSubmit={(e) => e.preventDefault()} className="item-enquiry__filters">
        <div className="field field--search">
          <label htmlFor="enq-keyword">Search</label>
          <input
            id="enq-keyword"
            type="text"
            placeholder="Item name or code..."
            {...register("keyword")}
          />
        </div>

        <div className="field">
          <label htmlFor="enq-category">Category</label>
          <select id="enq-category" {...register("category")}>
            <option value="">All categories</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="enq-status">Status</label>
          <select id="enq-status" {...register("status")}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="DISCONTINUED">Discontinued</option>
          </select>
        </div>

        <div className="field field--checkbox">
          <label htmlFor="enq-low-stock" className="checkbox-label">
            <input id="enq-low-stock" type="checkbox" {...register("lowStockOnly")} />
            <span>Low stock only</span>
          </label>
        </div>
      </form>

      {isLoading ? (
        <div className="loading-state">
          <span className="spinner" />
          Loading items...
        </div>
      ) : (
        <>
          <div className="item-enquiry__results-info">
            <span className="muted">
              {items.length} item{items.length !== 1 ? "s" : ""} found
              {isFetching && " (refreshing...)"}
            </span>
          </div>

          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Code</th>
                <th>Category</th>
                <th className="cell--num">Current Stock</th>
                <th className="cell--num">Reorder Lvl</th>
                <th className="cell--num">Max Stock</th>
                <th>Batches</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id} className={item.currentStock <= item.reorderLevel ? "row--warning" : ""}>
                  <td>{item.name}</td>
                  <td>{item.code}</td>
                  <td>{item.category ?? "—"}</td>
                  <td className="cell--num">
                    <span className={item.currentStock <= item.reorderLevel ? "text--warning" : ""}>
                      {item.currentStock}
                    </span>
                  </td>
                  <td className="cell--num">{item.reorderLevel}</td>
                  <td className="cell--num">{item.maxStock}</td>
                  <td>
                    {item.batches.length > 0 ? (
                      <details className="batch-details">
                        <summary>{item.batches.length} batch{item.batches.length !== 1 ? "es" : ""}</summary>
                        <table className="table table--compact">
                          <thead>
                            <tr>
                              <th>Batch</th>
                              <th>Expiry</th>
                              <th className="cell--num">Qty</th>
                            </tr>
                          </thead>
                          <tbody>
                            {item.batches.map((b) => (
                              <tr key={b.id}>
                                <td>{b.batchNumber}</td>
                                <td>{b.expiryDate}</td>
                                <td className="cell--num">{b.quantity}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </details>
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                  <td>
                    <span className={`badge badge--${item.status === "ACTIVE" ? "success" : item.status === "INACTIVE" ? "muted" : "error"}`}>
                      {item.status}
                    </span>
                  </td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr>
                  <td colSpan={8} className="muted">No items match your filters.</td>
                </tr>
              )}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}
