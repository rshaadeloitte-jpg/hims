import { useState } from "react";
import { useInventoryItems, useInventoryStores, useStockBalances } from "../queries";

export function ItemsTab() {
  const { data: items = [], isLoading } = useInventoryItems();

  if (isLoading) return <p className="muted">Loading items…</p>;

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Code</th>
          <th>Name</th>
          <th>Unit</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.id}>
            <td>{item.code}</td>
            <td>{item.name}</td>
            <td>{item.unitId}</td>
            <td>{item.status}</td>
          </tr>
        ))}
        {items.length === 0 && (
          <tr>
            <td colSpan={4} className="muted">No items.</td>
          </tr>
        )}
      </tbody>
    </table>
  );
}

export function StoresTab() {
  const { data: stores = [], isLoading } = useInventoryStores();

  if (isLoading) return <p className="muted">Loading stores…</p>;

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Code</th>
          <th>Name</th>
          <th>Location</th>
        </tr>
      </thead>
      <tbody>
        {stores.map((store) => (
          <tr key={store.id}>
            <td>{store.code}</td>
            <td>{store.name}</td>
            <td>{store.location ?? "—"}</td>
          </tr>
        ))}
        {stores.length === 0 && (
          <tr>
            <td colSpan={3} className="muted">No stores.</td>
          </tr>
        )}
      </tbody>
    </table>
  );
}

export function StockTab() {
  const [storeId, setStoreId] = useState("");
  const [itemId, setItemId] = useState("");

  const { data: balances = [], isLoading } = useStockBalances();

  if (isLoading) return <p className="muted">Loading stock…</p>;

  return (
    <div>
      <div className="row">
        <label>
          Store ID
          <input type="number" value={storeId} onChange={(e) => setStoreId(e.target.value)} />
        </label>
        <label>
          Item ID
          <input type="number" value={itemId} onChange={(e) => setItemId(e.target.value)} />
        </label>
      </div>
      <table className="table">
        <thead>
          <tr>
            <th>Item</th>
            <th>Batch</th>
            <th>Quantity</th>
            <th>Location</th>
          </tr>
        </thead>
        <tbody>
          {balances.map((b) => (
            <tr key={b.id}>
              <td>{b.itemId}</td>
              <td>{b.batchId}</td>
              <td>{b.quantity}</td>
              <td>{b.location}</td>
            </tr>
          ))}
          {balances.length === 0 && (
            <tr>
              <td colSpan={4} className="muted">No stock balances.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
