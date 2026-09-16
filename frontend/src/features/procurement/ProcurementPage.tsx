import { useState } from "react";
import { Button, HStack, Table, VStack } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";
import { useApprovePurchaseOrder, useCreatePurchaseOrder, usePurchaseOrders } from "./queries";
import { purchaseOrderSchema } from "./zodSchema";

export function ProcurementPage() {
  const { data: orders = [], isLoading } = usePurchaseOrders();
  const createOrder = useCreatePurchaseOrder();
  const approveOrder = useApprovePurchaseOrder();
  const [showForm, setShowForm] = useState(false);
  const [supplierId, setSupplierId] = useState("");
  const [items, setItems] = useState([{ itemId: "", quantity: "", unitPrice: "" }]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const parsedItems = items.map((i) => ({ itemId: Number(i.itemId), quantity: Number(i.quantity), unitPrice: Number(i.unitPrice) }));
    const result = purchaseOrderSchema.safeParse({ supplierId: Number(supplierId), items: parsedItems });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createOrder.mutate(result.data, { onSuccess: () => { setShowForm(false); setSupplierId(""); setItems([{ itemId: "", quantity: "", unitPrice: "" }]); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading purchase orders…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Procurement</h1>
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New PO"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Purchase Order</h2>
          <VStack gap={4} align="stretch" className="form-grid">
            <FormField label="Supplier ID" error={errors.supplierId}>
              <TextInput aria-label="Supplier ID" value={supplierId} onChange={(e) => setSupplierId(e.target.value)} />
            </FormField>
            <h3>Items</h3>
            {items.map((item, idx) => (
              <HStack key={idx} gap={2} className="item-row">
                <TextInput aria-label={`Item ${idx + 1} ID`} placeholder="Item ID" value={item.itemId} onChange={(e) => { const newItems = [...items]; newItems[idx].itemId = e.target.value; setItems(newItems); }} />
                <TextInput aria-label={`Item ${idx + 1} quantity`} placeholder="Qty" type="number" value={item.quantity} onChange={(e) => { const newItems = [...items]; newItems[idx].quantity = e.target.value; setItems(newItems); }} />
                <TextInput aria-label={`Item ${idx + 1} unit price`} placeholder="Unit Price" type="number" value={item.unitPrice} onChange={(e) => { const newItems = [...items]; newItems[idx].unitPrice = e.target.value; setItems(newItems); }} />
              </HStack>
            ))}
            <Button variant="secondary" onClick={() => setItems([...items, { itemId: "", quantity: "", unitPrice: "" }])}>Add Item</Button>
          </VStack>
          <Button variant="primary" onClick={handleSubmit} disabled={createOrder.isPending} loading={createOrder.isPending} loadingText="Saving…">
            Create PO
          </Button>
        </div>
      )}

      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">PO No</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Supplier</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Created</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row>
          </Table.Header>
          <Table.Body>
            {orders.map((o) => (
              <Table.Row key={o.id}>
                <Table.Cell>{o.poNo}</Table.Cell>
                <Table.Cell>{o.supplierName}</Table.Cell>
                <Table.Cell>{o.totalAmount}</Table.Cell>
                <Table.Cell>{o.status}</Table.Cell>
                <Table.Cell>{o.createdAt}</Table.Cell>
                <Table.Cell>
                  {o.status === "PENDING_APPROVAL" && (
                    <Button variant="primary" size="sm" onClick={() => approveOrder.mutate(o.id)} disabled={approveOrder.isPending} loading={approveOrder.isPending} loadingText="Approving…">
                      Approve
                    </Button>
                  )}
                </Table.Cell>
              </Table.Row>
            ))}
            {orders.length === 0 && <Table.Row><Table.Cell colSpan={6}>No purchase orders.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
