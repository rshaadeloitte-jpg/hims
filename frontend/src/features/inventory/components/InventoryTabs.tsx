import { useState } from "react";
import { HStack, Table } from "@chakra-ui/react";
import { FormField, TextInput } from "../../../components/ui";
import { useInventoryItems, useInventoryStores, useStockBalances } from "../queries";

export function ItemsTab() {
  const { data: items = [], isLoading } = useInventoryItems();

  if (isLoading) return <p className="muted">Loading items…</p>;

  return (
    <Table.Root size="sm" variant="line">
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Unit</Table.ColumnHeader>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {items.map((item) => (
          <Table.Row key={item.id}>
            <Table.Cell>{item.code}</Table.Cell>
            <Table.Cell>{item.name}</Table.Cell>
            <Table.Cell>{item.unitId}</Table.Cell>
            <Table.Cell>{item.status}</Table.Cell>
          </Table.Row>
        ))}
        {items.length === 0 && (
          <Table.Row>
            <Table.Cell colSpan={4}>No items.</Table.Cell>
          </Table.Row>
        )}
      </Table.Body>
    </Table.Root>
  );
}

export function StoresTab() {
  const { data: stores = [], isLoading } = useInventoryStores();

  if (isLoading) return <p className="muted">Loading stores…</p>;

  return (
    <Table.Root size="sm" variant="line">
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader>
          <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Location</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {stores.map((store) => (
          <Table.Row key={store.id}>
            <Table.Cell>{store.code}</Table.Cell>
            <Table.Cell>{store.name}</Table.Cell>
            <Table.Cell>{store.location ?? "—"}</Table.Cell>
          </Table.Row>
        ))}
        {stores.length === 0 && (
          <Table.Row>
            <Table.Cell colSpan={3}>No stores.</Table.Cell>
          </Table.Row>
        )}
      </Table.Body>
    </Table.Root>
  );
}

export function StockTab() {
  const [storeId, setStoreId] = useState("");
  const [itemId, setItemId] = useState("");

  const { data: balances = [], isLoading } = useStockBalances();

  if (isLoading) return <p className="muted">Loading stock…</p>;

  return (
    <div>
      <HStack gap={4} className="row">
        <FormField label="Store ID">
          <TextInput type="number" aria-label="Store ID" value={storeId} onChange={(e) => setStoreId(e.target.value)} />
        </FormField>
        <FormField label="Item ID">
          <TextInput type="number" aria-label="Item ID" value={itemId} onChange={(e) => setItemId(e.target.value)} />
        </FormField>
      </HStack>
      <Table.Root size="sm" variant="line">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Item</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Batch</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Quantity</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Location</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {balances.map((b) => (
            <Table.Row key={b.id}>
              <Table.Cell>{b.itemId}</Table.Cell>
              <Table.Cell>{b.batchId}</Table.Cell>
              <Table.Cell>{b.quantity}</Table.Cell>
              <Table.Cell>{b.location}</Table.Cell>
            </Table.Row>
          ))}
          {balances.length === 0 && (
            <Table.Row>
              <Table.Cell colSpan={4}>No stock balances.</Table.Cell>
            </Table.Row>
          )}
        </Table.Body>
      </Table.Root>
    </div>
  );
}
