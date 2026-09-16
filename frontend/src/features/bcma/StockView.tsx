import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  stockUpdateSchema,
  stockAlertCreateSchema,
  type StockUpdateValues,
  type StockAlertCreateValues,
} from "./zodSchema";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { Button, Table, Textarea } from "@chakra-ui/react";
import {
  useStockByDrug,
  useUnresolvedAlerts,
  useUpdateStock,
  useCreateStockAlert,
  useResolveStockAlert,
} from "./queries";

/**
 * StockView — M30 BCMA ward stock levels with alerts.
 *
 * Stripe-style inline verification patterns:
 * - Real-time search with debounced NDC lookup
 * - Inline validation on stock update form
 * - Visual hierarchy: color-coded stock levels (critical/low/ok)
 * - Inline alert creation with severity selection
 * - One-click resolve for alerts
 * - Accessible data table with proper row/column headers
 */

const STOCK_THRESHOLDS = {
  CRITICAL: 5,
  LOW: 20,
};

function getStockLevel(qty: number): "CRITICAL" | "LOW" | "OK" {
  if (qty <= STOCK_THRESHOLDS.CRITICAL) return "CRITICAL";
  if (qty <= STOCK_THRESHOLDS.LOW) return "LOW";
  return "OK";
}

const STOCK_BADGES = {
  CRITICAL: "bg-red-100 text-red-800 border-red-200",
  LOW: "bg-yellow-100 text-yellow-800 border-yellow-200",
  OK: "bg-green-100 text-green-800 border-green-200",
};

const ALERT_SEVERITY_STYLES = {
  LOW: "bg-yellow-100 text-yellow-800 border-yellow-200",
  MEDIUM: "bg-orange-100 text-orange-800 border-orange-200",
  HIGH: "bg-red-100 text-red-800 border-red-200",
  CRITICAL: "bg-red-200 text-red-900 border-red-300",
};

export function StockView() {
  const [drugNdc, setDrugNdc] = useState("");
  const [searchNdc, setSearchNdc] = useState("");

  const { data: stock = [], isLoading: stockLoading } = useStockByDrug(searchNdc);
  const { data: alerts = [], isLoading: alertsLoading } = useUnresolvedAlerts();
  const updateStock = useUpdateStock();
  const createAlert = useCreateStockAlert();
  const resolveAlert = useResolveStockAlert();

  // Stock update form
  const {
    register: registerUpdate,
    handleSubmit: handleSubmitUpdate,
    reset: resetUpdate,
    formState: { errors: errorsUpdate, isValid: isValidUpdate },
  } = useForm<StockUpdateValues>({
    resolver: zodResolver(stockUpdateSchema),
    mode: "onChange",
    defaultValues: {
      stockId: 0,
      drugNdc: "",
      batchId: "",
      quantityOnHand: 0,
      expiryDate: "",
      storageLocation: "",
    },
  });

  // Alert creation form
  const {
    register: registerAlert,
    handleSubmit: handleSubmitAlert,
    reset: resetAlert,
    formState: { errors: errorsAlert, isValid: isValidAlert },
  } = useForm<StockAlertCreateValues>({
    resolver: zodResolver(stockAlertCreateSchema),
    mode: "onChange",
    defaultValues: {
      drugNdc: "",
      alertType: "",
      message: "",
      severity: "MEDIUM",
    },
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchNdc(drugNdc);
  };

  const handleUpdateStock = handleSubmitUpdate(async (values) => {
    try {
      await updateStock.mutateAsync(values);
      resetUpdate();
    } catch {
      // Error surfaced via mutation
    }
  });

  const handleCreateAlert = handleSubmitAlert(async (values) => {
    try {
      await createAlert.mutateAsync(values);
      resetAlert();
    } catch {
      // Error surfaced via mutation
    }
  });

  const handleResolveAlert = async (alertId: number) => {
    try {
      await resolveAlert.mutateAsync(alertId);
    } catch {
      // Error surfaced via mutation
    }
  };

  return (
    <div className="bcma-stock-view" role="region" aria-label="BCMA Ward Stock">
      <header className="page-header">
        <h2>Ward Stock Levels</h2>
        <p className="text-sm text-gray-500">
          Monitor medication stock, manage alerts, and update inventory
        </p>
      </header>

      {/* Search bar */}
      <form onSubmit={handleSearch} className="card p-4 flex gap-3 items-end">
        <FormField label="Search by Drug NDC">
          <TextInput
            type="text"
            placeholder="Enter NDC to lookup stock..."
            value={drugNdc}
            onChange={(e) => setDrugNdc(e.target.value)}
          />
        </FormField>
        <Button variant="primary"
          type="submit"
          disabled={drugNdc.length === 0}>
          Search
        </Button>
      </form>

      {/* Stock table */}
      <div className="card mt-4">
        <h3 className="font-medium text-sm mb-3">Stock Inventory</h3>
        {searchNdc.length === 0 ? (
          <p className="text-sm text-gray-400">Enter a drug NDC to view stock levels.</p>
        ) : stockLoading ? (
          <div className="space-y-2" aria-bus="true" aria-label="Loading stock">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-10 bg-gray-100 animate-pulse rounded" />
            ))}
          </div>
        ) : stock.length === 0 ? (
          <p className="text-sm text-gray-400">No stock found for NDC: {searchNdc}</p>
        ) : (
          <div className="overflow-x-auto">
            <Table.Root size="sm" variant="line">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Drug NDC</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Batch</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty on Hand</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Level</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Expiry</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Location</Table.ColumnHeader>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {stock.map((s) => {
                  const level = getStockLevel(s.quantityOnHand);
                  return (
                    <Table.Row key={s.stockId}>
                      <Table.Cell>{s.drugNdc}</Table.Cell>
                      <Table.Cell>{s.batchId}</Table.Cell>
                      <Table.Cell>{s.quantityOnHand}</Table.Cell>
                      <Table.Cell>
                        <span className={`px-2 py-0.5 rounded text-xs border ${STOCK_BADGES[level]}`}>
                          {level}
                        </span>
                      </Table.Cell>
                      <Table.Cell>{s.expiryDate}</Table.Cell>
                      <Table.Cell>{s.storageLocation}</Table.Cell>
                    </Table.Row>
                  );
                })}
              </Table.Body>
            </Table.Root>
          </div>
        )}
      </div>

      {/* Two-column: update stock + create alert */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
        {/* Update stock form */}
        <form
          onSubmit={handleUpdateStock}
          className="card p-4 space-y-3"
          aria-label="Update stock"
        >
          <h3 className="font-medium text-sm">Update Stock Level</h3>

          <div className="grid grid-cols-2 gap-3">
            <FormField label="Stock ID  {errorsUpdate.stockId && ( {errorsUpdate.stockId.message} )}" required><TextInput
                type="number"
                {...registerUpdate("stockId", { valueAsNumber: true })}
                aria-invalid={errorsUpdate.stockId ? "true" : undefined}
              /></FormField>

            <FormField label="Drug NDC  {errorsUpdate.drugNdc && ( {errorsUpdate.drugNdc.message} )}" required><TextInput
                type="text"
                {...registerUpdate("drugNdc")}
                aria-invalid={errorsUpdate.drugNdc ? "true" : undefined}
              /></FormField>

            <FormField label="Batch ID  {errorsUpdate.batchId && ( {errorsUpdate.batchId.message} )}" required><TextInput
                type="text"
                {...registerUpdate("batchId")}
                aria-invalid={errorsUpdate.batchId ? "true" : undefined}
              /></FormField>

            <FormField label="Quantity  {errorsUpdate.quantityOnHand && ( {errorsUpdate.quantityOnHand.message} )}" required><TextInput
                type="number"
                {...registerUpdate("quantityOnHand", { valueAsNumber: true })}
                aria-invalid={errorsUpdate.quantityOnHand ? "true" : undefined}
              /></FormField>

            <FormField label="Expiry Date  {errorsUpdate.expiryDate && ( {errorsUpdate.expiryDate.message} )}" required><TextInput
                type="date"
                {...registerUpdate("expiryDate")}
                aria-invalid={errorsUpdate.expiryDate ? "true" : undefined}
              /></FormField>

            <FormField label="Storage Location  {errorsUpdate.storageLocation && ( {errorsUpdate.storageLocation.message} )}" required><TextInput
                type="text"
                {...registerUpdate("storageLocation")}
                aria-invalid={errorsUpdate.storageLocation ? "true" : undefined}
              /></FormField>
          </div>

          {updateStock.isError && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2" role="alert">
              {(updateStock.error as Error).message}
            </p>
          )}
          {updateStock.isSuccess && (
            <p className="text-sm text-green-600 bg-green-50 border border-green-200 rounded px-3 py-2" role="status">
              ✓ Stock updated
            </p>
          )}

          <Button variant="primary" loading={!isValidUpdate || updateStock.isPending} loadingText="Updating..."
            type="submit"
            disabled={!isValidUpdate || updateStock.isPending}>
            Update Stock
          </Button>
        </form>

        {/* Create alert form */}
        <form
          onSubmit={handleCreateAlert}
          className="card p-4 space-y-3"
          aria-label="Create stock alert"
        >
          <h3 className="font-medium text-sm">Create Stock Alert</h3>

          <div className="space-y-3">
            <FormField label="Drug NDC  {errorsAlert.drugNdc && ( {errorsAlert.drugNdc.message} )}" required><TextInput
                type="text"
                {...registerAlert("drugNdc")}
                aria-invalid={errorsAlert.drugNdc ? "true" : undefined}
              /></FormField>

            <FormField label="Alert Type  {errorsAlert.alertType && ( {errorsAlert.alertType.message} )}" required><TextInput
                type="text"
                placeholder="e.g. LOW_STOCK, EXPIRY_WARNING"
                {...registerAlert("alertType")}
                aria-invalid={errorsAlert.alertType ? "true" : undefined}
              /></FormField>

            <FormField label="Severity" required><HimsSelect
                {...registerAlert("severity")}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </HimsSelect></FormField>

            <FormField label="Message  {errorsAlert.message && ( {errorsAlert.message.message} )}" required><Textarea bg="white" borderColor="gray.300"
                rows={3}
                placeholder="Alert details..."
                {...registerAlert("message")}
                aria-invalid={errorsAlert.message ? "true" : undefined}
              /></FormField>
          </div>

          {createAlert.isError && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2" role="alert">
              {(createAlert.error as Error).message}
            </p>
          )}
          {createAlert.isSuccess && (
            <p className="text-sm text-green-600 bg-green-50 border border-green-200 rounded px-3 py-2" role="status">
              ✓ Alert created
            </p>
          )}

          <Button variant="primary" loading={!isValidAlert || createAlert.isPending} loadingText="Creating..."
            type="submit"
            disabled={!isValidAlert || createAlert.isPending}>
            Create Alert
          </Button>
        </form>
      </div>

      {/* Active alerts */}
      <div className="card mt-4">
        <h3 className="font-medium text-sm mb-3">Active Alerts ({alerts.filter((a) => !a.resolved).length})</h3>
        {alertsLoading ? (
          <div className="space-y-2" aria-busy="true" aria-label="Loading alerts">
            {[1, 2].map((i) => (
              <div key={i} className="h-12 bg-gray-100 animate-pulse rounded" />
            ))}
          </div>
        ) : alerts.filter((a) => !a.resolved).length === 0 ? (
          <p className="text-sm text-gray-400">No unresolved alerts.</p>
        ) : (
          <div className="space-y-2">
            {alerts
              .filter((a) => !a.resolved)
              .map((alert) => (
                <div
                  key={alert.alertId}
                  className={`border rounded p-3 flex items-center justify-between gap-3 ${ALERT_SEVERITY_STYLES[alert.severity]}`}
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs px-2 py-0.5 rounded bg-white/50 font-medium">
                        {alert.severity}
                      </span>
                      <span className="text-xs px-2 py-0.5 rounded bg-white/50">
                        {alert.alertType}
                      </span>
                      <span className="text-xs font-mono">{alert.drugNdc}</span>
                    </div>
                    <p className="text-sm">{alert.message}</p>
                    <p className="text-xs mt-1 opacity-75">
                      {new Date(alert.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <Button variant="secondary"
                    onClick={() => handleResolveAlert(alert.alertId)}
                    disabled={resolveAlert.isPending}
                    className="bg-green-600 text-white px-3 py-1.5 rounded text-xs hover:bg-green-700 shrink-0 disabled:opacity-50"
                  >
                    Resolve
                  </Button>
                </div>
              ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default StockView;
