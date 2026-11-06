import { useState, useRef, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { Prescription, PrescriptionItem, ItemSale, ItemSaleDetail } from "./types";
import { useItemSales, useCreateItemSale, useDispenseItemSale } from "./queries";
import { Box, Button, VStack, HStack, Input, Text } from "@chakra-ui/react";

interface DispenseFormProps {
  prescription: Prescription;
  onClose?: () => void;
}

function pendingOf(item: PrescriptionItem): number {
  const v = (item as any).pendingQty ?? (item as any).pendingQuantity ?? item.qty;
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

export function DispenseForm({ prescription, onClose }: DispenseFormProps) {
  const qc = useQueryClient();
  const salesQuery = useItemSales(prescription.prescriptionId);
  const createSale = useCreateItemSale();
  const dispense = useDispenseItemSale();

  const existingSale: ItemSale | null = (salesQuery.data ?? [])[0] ?? null;
  const isExisting = !!existingSale;
  const fullyDispensed = !!existingSale?.dispensed || existingSale?.statusCode === 3;

  const sourceLines: any[] = isExisting
    ? (existingSale?.details ?? [])
    : (prescription.details ?? []);

  const [qty, setQty] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [saleId, setSaleId] = useState<number | null>(null);
  const submittingRef = useRef(false);
  const [submitting, setSubmitting] = useState(false);

  // Seed qty from the active source (existing sale pendingQty, or prescription qty).
  useEffect(() => {
    const init: Record<number, string> = {};
    if (isExisting) {
      (existingSale?.details ?? []).forEach((d: ItemSaleDetail) => {
        init[d.lineNo] = String(d.pendingQty ?? 0);
      });
    } else {
      (prescription.details ?? []).forEach((it, idx) => {
        const key = it.lineNo ?? idx + 1;
        init[key] = String(pendingOf(it));
      });
    }
    setQty(init);
    // reset saleId when switching prescriptions
    setSaleId(existingSale?.saleId ?? null);
    setError(null);
    setSuccess(null);
  }, [prescription.prescriptionId, existingSale?.saleId]);

  const validate = (): string | null => {
    if (sourceLines.length === 0) return "No prescription lines";
    for (const line of sourceLines) {
      const key = line.lineNo;
      const pending = isExisting ? (line.pendingQty ?? 0) : pendingOf(line);
      if (!Number.isInteger(pending)) {
        return "Fractional quantities are not supported; dispense in whole units";
      }
      const raw = qty[key];
      const n = Number(raw);
      if (!Number.isFinite(n) || n <= 0) return "Quantity must be > 0";
      if (n > pending) return `Quantity must be <= pending (${pending})`;
    }
    if (fullyDispensed) return "Prescription fully dispensed";
    return null;
  };

  const buildDispenseLines = () =>
    sourceLines.map((line) => ({
      lineNo: line.lineNo,
      qty: Number(qty[line.lineNo]),
    }));

  const doDispense = async (id: number) => {
    await dispense.mutateAsync({ saleId: id, lines: buildDispenseLines() });
  };

  const handleConfirm = async () => {
    if (submittingRef.current) return;
    if (fullyDispensed) {
      setError("Prescription fully dispensed");
      return;
    }
    setError(null);
    setSuccess(null);
    const v = validate();
    if (v) { setError(v); return; }
    submittingRef.current = true;
    setSubmitting(true);
    let createdId: number | null = saleId ?? existingSale?.saleId ?? null;
    try {
      if (createdId == null) {
        try {
          const sale = await createSale.mutateAsync({
            patientId: prescription.patientId,
            prescriptionId: prescription.prescriptionId,
            lines: (prescription.details ?? []).map((it) => ({
              itemId: it.itemId,
              qty: pendingOf(it),
            })),
          });
          createdId = (sale as any)?.saleId ?? null;
          if (createdId == null) throw new Error("Sale created but no sale id returned");
          setSaleId(createdId);
        } catch (e) {
          const err = e as ApiError;
          if (err instanceof ApiError && err.code === "PH_PRESCRIPTION_HAS_SALE") {
            await salesQuery.refetch?.();
            return;
          }
          throw e;
        }
      }
      if (createdId == null) throw new Error("No sale id");
      setSaleId(createdId);
      await doDispense(createdId);
      setSuccess(`Dispensed successfully (sale #${createdId})`);
      setError(null);
      qc.invalidateQueries({ queryKey: ["pharmacy"] });
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : (e as Error)?.message ?? "Request failed";
      if (createdId != null) {
        setError(`Sale #${createdId} created, but dispense failed: ${msg}`);
      } else {
        setError(msg);
      }
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  const handleRetry = async () => {
    if (submittingRef.current || saleId == null) return;
    setError(null);
    setSuccess(null);
    const v = validate();
    if (v) { setError(v); return; }
    submittingRef.current = true;
    setSubmitting(true);
    try {
      await doDispense(saleId);
      setSuccess(`Dispensed successfully (sale #${saleId})`);
      setError(null);
      qc.invalidateQueries({ queryKey: ["pharmacy"] });
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : (e as Error)?.message ?? "Request failed";
      setError(`Sale #${saleId} created, but dispense failed: ${msg}`);
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  return (
    <Box mt={6} p={4} bg="gray.50" borderRadius="lg">
      <Text fontWeight="bold" mb={2}>Dispense Prescription #{prescription.prescriptionId}</Text>
      {salesQuery.isLoading && <Text color="gray.500">Loading sale…</Text>}
      {fullyDispensed && <Text color="green.500" mb={2}>Fully dispensed</Text>}
      {sourceLines.length === 0 && <Text color="gray.500">No items</Text>}
      <VStack gap={3} align="stretch">
        {sourceLines.map((it: any) => {
          const key = it.lineNo;
          const pending = isExisting ? (it.pendingQty ?? 0) : pendingOf(it);
          return (
            <HStack key={key} gap={3}>
              <Text flex="1">Item {it.itemId} (pending: {pending})</Text>
              <Input
                type="number"
                width="100px"
                aria-label={`Quantity for item ${it.itemId}`}
                data-testid={`qty-${it.lineNo}`}
                value={qty[key] ?? ""}
                min={1}
                max={pending}
                onChange={(e) => setQty((q) => ({ ...q, [key]: e.target.value }))}
                disabled={submitting || fullyDispensed}
              />
            </HStack>
          );
        })}
      </VStack>
      {error && <Text color="red.500" mt={3} role="alert">{error}</Text>}
      {saleId != null && error && (
        <Text mt={1} fontSize="sm">Sale ID: {saleId}</Text>
      )}
      {success && <Text color="green.500" mt={3}>{success}</Text>}
      <HStack mt={4} gap={3}>
        <Button
          variant="primary"
          data-testid="confirm-dispense"
          onClick={handleConfirm}
          disabled={submitting || sourceLines.length === 0 || fullyDispensed}
          loading={submitting}
        >
          Confirm Dispense
        </Button>
        {saleId != null && error && (
          <Button variant="secondary" data-testid="retry-dispense" onClick={handleRetry} disabled={submitting}>
            Retry Dispense
          </Button>
        )}
        {onClose && (
          <Button variant="ghost" onClick={onClose} disabled={submitting}>
            Close
          </Button>
        )}
      </HStack>
    </Box>
  );
}
