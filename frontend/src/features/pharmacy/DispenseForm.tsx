import { useState, useRef, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { Prescription, PrescriptionItem, ItemSale, ItemSaleDetail, AvailableBatch } from "./types";
import { useItemSales, useCreateItemSale, useDispenseItemSale } from "./queries";
import { Box, Button, VStack, HStack, Input, Text } from "@chakra-ui/react";

interface DispenseFormProps {
  prescription: Prescription;
  onClose?: () => void;
}

interface FormLine {
  lineNo: number;
  itemId: number;
  pending: number;
  batchId?: string | null;
}

function pendingOf(item: PrescriptionItem): number {
  const v = item.pendingQty ?? item.qty;
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

function totalAvailable(batches: AvailableBatch[]): number {
  return batches.reduce((s, b) => s + Number(b.availableQty ?? 0), 0);
}

function allocateFeFo(qty: number, batches: AvailableBatch[]): { batch: AvailableBatch; qty: number }[] {
  const result = batches.map((b) => ({ batch: b, qty: 0 }));
  let remaining = qty;
  for (const r of result) {
    if (remaining <= 0) break;
    const take = Math.min(Number(r.batch.availableQty ?? 0), remaining);
    r.qty = take;
    remaining -= take;
  }
  return result;
}

export function DispenseForm({ prescription, onClose }: DispenseFormProps) {
  const qc = useQueryClient();
  const salesQuery = useItemSales(prescription.prescriptionId);
  const createSale = useCreateItemSale();
  const dispense = useDispenseItemSale();

  const existingSale: ItemSale | null = (salesQuery.data ?? [])[0] ?? null;
  const isExisting = !!existingSale;
  const fullyDispensed = !!existingSale?.dispensed || existingSale?.statusCode === 3;

  const sourceLines: FormLine[] = isExisting
    ? (existingSale?.details ?? []).map((d: ItemSaleDetail) => ({
        lineNo: d.lineNo,
        itemId: d.itemId,
        pending: d.pendingQty ?? 0,
        batchId: d.batchId,
      }))
    : (prescription.details ?? []).map((it: PrescriptionItem, idx: number) => ({
        lineNo: it.lineNo ?? idx + 1,
        itemId: it.itemId,
        pending: pendingOf(it),
        batchId: undefined,
      }));

  const [qty, setQty] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [saleId, setSaleId] = useState<number | null>(null);
  const submittingRef = useRef(false);
  const [submitting, setSubmitting] = useState(false);

  // Batch availability for create mode.
  const [batchesByItem, setBatchesByItem] = useState<Record<number, AvailableBatch[]>>({});
  const [allocations, setAllocations] = useState<Record<number, Record<string, string>>>({});
  const [loadingBatches, setLoadingBatches] = useState(false);
  const [offerQty, setOfferQty] = useState<number | null>(null);

  // Seed qty from the active source.
  useEffect(() => {
    const init: Record<number, string> = {};
    if (isExisting) {
      (existingSale?.details ?? []).forEach((d: ItemSaleDetail) => {
        init[d.lineNo] = String(d.pendingQty ?? 0);
      });
    } else {
      (prescription.details ?? []).forEach((it: PrescriptionItem, idx: number) => {
        const key = it.lineNo ?? idx + 1;
        init[key] = String(pendingOf(it));
      });
    }
    setQty(init);
    setSaleId(existingSale?.saleId ?? null);
    setError(null);
    setSuccess(null);
  }, [prescription.prescriptionId, existingSale?.saleId]);

  // Fetch available batches for each line item in create mode.
  useEffect(() => {
    if (isExisting) return;
    const items: PrescriptionItem[] = prescription.details ?? [];
    const unique = Array.from(new Set(items.map((it) => it.itemId)));
    if (unique.length === 0) return;
    let cancelled = false;
    setLoadingBatches(true);
    Promise.all(
      unique.map(async (itemId: number) => {
        const res = await fetch(`/api/v1/item-sales/available-batches?itemId=${itemId}`);
        const body: AvailableBatch[] = await res.json().catch(() => []);
        return [itemId, Array.isArray(body) ? body : []] as const;
      }),
    )
      .then((entries) => {
        if (cancelled) return;
        const map: Record<number, AvailableBatch[]> = {};
        for (const [itemId, batches] of entries) map[itemId] = batches;
        setBatchesByItem(map);
        const alloc: Record<number, Record<string, string>> = {};
        for (const it of items) {
          const key = it.lineNo;
          const q = pendingOf(it);
          const batches = map[it.itemId] ?? [];
          const fefo = allocateFeFo(q, batches);
          alloc[key] = {};
          for (const a of fefo) {
            alloc[key][String(a.batch.batchId)] = String(a.qty);
          }
        }
        setAllocations(alloc);
        setLoadingBatches(false);
      })
      .catch(() => {
        if (!cancelled) setLoadingBatches(false);
      });
    return () => {
      cancelled = true;
    };
  }, [prescription.prescriptionId, isExisting]);

  const validate = (): string | null => {
    if (sourceLines.length === 0) return "No prescription lines";
    for (const line of sourceLines) {
      const key = line.lineNo;
      const pending = line.pending;
      if (!Number.isInteger(pending)) {
        return "Fractional quantities are not supported; dispense in whole units";
      }
      const raw = qty[key];
      const n = Number(raw);
      if (!Number.isFinite(n) || n <= 0) return "Quantity must be > 0";
      if (n > pending) return `Quantity must be <= pending (${pending})`;
    }
    if (fullyDispensed) return "Prescription fully dispensed";
    // Stock availability check in create mode.
    if (!isExisting) {
      for (const line of sourceLines) {
        const itemId = line.itemId;
        const batches = batchesByItem[itemId] ?? [];
        if (batches.length === 0) continue;
        const alloc = allocations[line.lineNo] ?? {};
        for (const b of batches) {
          const a = Number(alloc[String(b.batchId)] ?? 0);
          if (!Number.isInteger(a) || a < 0) {
            setOfferQty(null);
            return "Allocation must be a non-negative integer";
          }
          if (a > Number(b.availableQty ?? 0)) {
            setOfferQty(null);
            return "Allocation must be <= available quantity";
          }
        }
        const total = totalAvailable(batches);
        const needed = line.pending;
        if (total < needed) {
          setOfferQty(total);
          return `Insufficient stock: only ${total} available of ${needed} requested`;
        }
      }
    }
    return null;
  };

  const buildDispenseLines = () =>
    sourceLines.map((line) => ({
      lineNo: line.lineNo,
      qty: Number(qty[line.lineNo]),
    }));

  const buildCreateLines = () => {
    const lines: { itemId: number; qty: number; batchId?: string }[] = [];
    if (isExisting) return lines;
    for (const it of prescription.details ?? []) {
      const itemId = it.itemId;
      const key = it.lineNo;
      const batches = batchesByItem[itemId] ?? [];
      const alloc = allocations[key] ?? {};
      if (batches.length === 0) {
        lines.push({ itemId, qty: pendingOf(it) });
        continue;
      }
      for (const b of batches) {
        const a = Number(alloc[String(b.batchId)] ?? 0);
        if (a > 0) {
          lines.push({ itemId, qty: a, batchId: String(b.batchId) });
        }
      }
    }
    return lines;
  };

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
          const saleLines = buildCreateLines();
          const sale = await createSale.mutateAsync({
            patientId: prescription.patientId,
            prescriptionId: prescription.prescriptionId,
            lines: saleLines,
          });
          createdId = sale?.saleId ?? null;
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
      const msg = e instanceof ApiError ? e.message : (e instanceof Error ? e.message : "Request failed");
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
      const msg = e instanceof ApiError ? e.message : (e instanceof Error ? e.message : "Request failed");
      setError(`Sale #${saleId} created, but dispense failed: ${msg}`);
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  const handleDispenseAvailable = () => {
    if (offerQty == null) return;
    // Reduce each line's dispense qty to available total and reallocate.
    const newQty: Record<number, string> = { ...qty };
    const newAlloc: Record<number, Record<string, string>> = { ...allocations };
    for (const line of sourceLines) {
      const key = line.lineNo;
      const batches = batchesByItem[line.itemId] ?? [];
      const total = totalAvailable(batches);
      newQty[key] = String(total);
      const fefo = allocateFeFo(total, batches);
      newAlloc[key] = {};
      for (const a of fefo) {
        newAlloc[key][String(a.batch.batchId)] = String(a.qty);
      }
    }
    setQty(newQty);
    setAllocations(newAlloc);
    setError(null);
    setOfferQty(null);
  };

  return (
    <Box mt={6} p={4} bg="gray.50" borderRadius="lg">
      <Text fontWeight="bold" mb={2}>Dispense Prescription #{prescription.prescriptionId}</Text>
      {salesQuery.isLoading && <Text color="gray.500">Loading sale…</Text>}
      {loadingBatches && <Text color="gray.500">Loading batches…</Text>}
      {fullyDispensed && <Text color="green.500" mb={2}>Fully dispensed</Text>}
      {sourceLines.length === 0 && <Text color="gray.500">No items</Text>}
      <VStack gap={3} align="stretch">
        {sourceLines.map((it: FormLine) => {
          const key = it.lineNo;
          const pending = it.pending;
          const batches: AvailableBatch[] = isExisting ? [] : (batchesByItem[it.itemId] ?? []);
          return (
            <Box key={key} p={3} bg="white" borderRadius="md" borderWidth="1px">
              <HStack mb={2}>
                <Text flex="1">Item {it.itemId} (pending: {pending})</Text>
                {isExisting && it.batchId != null && (
                  <Text fontSize="sm" color="gray.600">Batch {it.batchId}</Text>
                )}
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
              {!isExisting && batches.length > 0 && (
                <VStack align="stretch" gap={2} mt={2}>
                  {batches.map((b) => (
                    <HStack key={String(b.batchId)} gap={3}>
                      <Text flex="1" fontSize="sm">
                        Batch {b.batchCode ?? b.batchId} (exp: {b.expiryDate ?? "—"}, avail: {b.availableQty})
                      </Text>
                      <Input
                        type="number"
                        width="90px"
                        aria-label={`Allocation for batch ${b.batchCode ?? b.batchId}`}
                        data-testid={`batch-qty-${it.lineNo}-${b.batchId}`}
                        value={allocations[key]?.[String(b.batchId)] ?? "0"}
                        min={0}
                        max={Number(b.availableQty ?? 0)}
                        onChange={(e) =>
                          setAllocations((a) => ({
                            ...a,
                            [key]: { ...(a[key] ?? {}), [String(b.batchId)]: e.target.value },
                          }))
                        }
                        disabled={submitting || fullyDispensed}
                      />
                    </HStack>
                  ))}
                </VStack>
              )}
            </Box>
          );
        })}
      </VStack>
      {error && <Text color="red.500" mt={3} role="alert">{error}</Text>}
      {offerQty != null && (
        <Button mt={2} size="sm" variant="secondary" onClick={handleDispenseAvailable} disabled={submitting}>
          Dispense available ({offerQty})
        </Button>
      )}
      {saleId != null && error && (
        <Text mt={1} fontSize="sm">Sale ID: {saleId}</Text>
      )}
      {success && <Text color="green.500" mt={3}>{success}</Text>}
      <HStack mt={4} gap={3}>
        <Button
          variant="primary"
          data-testid="confirm-dispense"
          onClick={handleConfirm}
          disabled={submitting || sourceLines.length === 0 || fullyDispensed || loadingBatches}
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
