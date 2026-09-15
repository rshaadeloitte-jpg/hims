import { useState, useCallback } from "react";
import { useRadStudies, useCreateRadOrder, useRadOrders, useRescheduleLine, useRequestRetest } from "./queries";
import { radOrderBookingSchema } from "./zodSchema";
import { MODALITIES, type Modality, type RadStudyItem, type RadOrder } from "./types";

interface RadOrderBookingProps {
  patientId?: number;
  onBooked?: (orderId: number) => void;
}

interface LineEntry {
  itemId: number;
  qty: number;
}

/**
 * M16 RadOrderBooking — Linear-style study selection + reschedule/retest.
 * Left: filterable study list with modality badges. Right: order summary + actions.
 */
export function RadOrderBooking({ patientId, onBooked }: RadOrderBookingProps) {
  const [selectedStudies, setSelectedStudies] = useState<Set<number>>(new Set());
  const [searchQuery, setSearchQuery] = useState("");
  const [modalityFilter, setModalityFilter] = useState<Modality | "">("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    patientId: patientId ? String(patientId) : "",
    encounterId: "",
    admissionId: "",
    modality: "XRAY" as Modality,
    priority: "ROUTINE" as "ROUTINE" | "URGENT",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [lines, setLines] = useState<LineEntry[]>([]);

  const { data: studies = [], isLoading } = useRadStudies();
  const { data: orders = [] } = useRadOrders(patientId ? { patientId } : undefined);
  const createOrder = useCreateRadOrder();
  const rescheduleLine = useRescheduleLine();
  const requestRetest = useRequestRetest();

  const toggleStudy = useCallback((itemId: number) => {
    setSelectedStudies((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) {
        next.delete(itemId);
        setLines((l) => l.filter((line) => line.itemId !== itemId));
      } else {
        next.add(itemId);
        setLines((l) => [...l, { itemId, qty: 1 }]);
      }
      return next;
    });
  }, []);

  const updateLineQty = useCallback((itemId: number, qty: number) => {
    setLines((l) => l.map((line) => (line.itemId === itemId ? { ...line, qty } : line)));
  }, []);

  const handleSubmit = () => {
    const result = radOrderBookingSchema.safeParse({
      ...form,
      patientId: Number(form.patientId),
      encounterId: form.encounterId ? Number(form.encounterId) : undefined,
      admissionId: form.admissionId ? Number(form.admissionId) : undefined,
      lines,
    });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path.join(".")] = e.message;
      });
      setErrors(fieldErrors);
      return;
    }
    createOrder.mutate(result.data, {
      onSuccess: (order) => {
        setShowForm(false);
        setSelectedStudies(new Set());
        setLines([]);
        setErrors({});
        onBooked?.(order.radOrderId);
      },
    });
  };

  const handleReschedule = async (orderId: number, lineNo: number, rescheduledTo: string) => {
    try {
      await rescheduleLine.mutateAsync({ orderId, lineNo, rescheduledTo });
    } catch (err) {
      console.error("Reschedule failed:", err);
    }
  };

  const handleRetest = async (orderId: number, lineNo: number, reason: string) => {
    try {
      await requestRetest.mutateAsync({ orderId, lineNo, reason });
    } catch (err) {
      console.error("Retest request failed:", err);
    }
  };

  const filteredStudies = studies.filter((s) => {
    const matchesSearch =
      !searchQuery ||
      s.itemName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.itemCode.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesModality = !modalityFilter || s.modality === modalityFilter;
    return matchesSearch && matchesModality;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Radiology Orders</h2>
          <p className="text-sm text-gray-500">Book studies, reschedule, or request retests</p>
        </div>
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? "Cancel" : "New Order"}
        </button>
      </header>

      {/* Booking Form */}
      {showForm && (
        <div className="border border-gray-200 rounded-lg p-6 space-y-4">
          <h3 className="text-lg font-medium text-gray-900">New Radiology Order</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="rad-patientId" className="block text-xs font-medium text-gray-700 mb-1">
                Patient ID *
              </label>
              <input
                id="rad-patientId"
                type="number"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                value={form.patientId}
                onChange={(e) => setForm({ ...form, patientId: e.target.value })}
              />
              {errors.patientId && <p className="text-xs text-red-600 mt-1">{errors.patientId}</p>}
            </div>

            <div>
              <label htmlFor="rad-modality" className="block text-xs font-medium text-gray-700 mb-1">
                Modality *
              </label>
              <select
                id="rad-modality"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                value={form.modality}
                onChange={(e) => setForm({ ...form, modality: e.target.value as Modality })}
              >
                {MODALITIES.map((m) => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
              {errors.modality && <p className="text-xs text-red-600 mt-1">{errors.modality}</p>}
            </div>

            <div>
              <label htmlFor="rad-encounterId" className="block text-xs font-medium text-gray-700 mb-1">
                Encounter ID
              </label>
              <input
                id="rad-encounterId"
                type="number"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                value={form.encounterId}
                onChange={(e) => setForm({ ...form, encounterId: e.target.value })}
              />
              {errors.encounterId && <p className="text-xs text-red-600 mt-1">{errors.encounterId}</p>}
            </div>

            <div>
              <label htmlFor="rad-admissionId" className="block text-xs font-medium text-gray-700 mb-1">
                Admission ID
              </label>
              <input
                id="rad-admissionId"
                type="number"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                value={form.admissionId}
                onChange={(e) => setForm({ ...form, admissionId: e.target.value })}
              />
              {errors.admissionId && <p className="text-xs text-red-600 mt-1">{errors.admissionId}</p>}
            </div>

            <div>
              <label htmlFor="rad-priority" className="block text-xs font-medium text-gray-700 mb-1">
                Priority
              </label>
              <select
                id="rad-priority"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value as "ROUTINE" | "URGENT" })}
              >
                <option value="ROUTINE">Routine</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
          </div>

          {/* Study Selection Table */}
          <div className="border-t border-gray-200 pt-4">
            <h4 className="text-sm font-medium text-gray-900 mb-3">Select Studies</h4>

            <div className="flex gap-3 mb-3">
              <input
                type="text"
                placeholder="Search studies..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
              />
              <select
                value={modalityFilter}
                onChange={(e) => setModalityFilter(e.target.value as Modality | "")}
                className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
              >
                <option value="">All Modalities</option>
                {MODALITIES.map((m) => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
            </div>

            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                      <input
                        type="checkbox"
                        checked={selectedStudies.size === filteredStudies.length && filteredStudies.length > 0}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedStudies(new Set(filteredStudies.map((s) => s.itemId)));
                            setLines(filteredStudies.map((s) => ({ itemId: s.itemId, qty: 1 })));
                          } else {
                            setSelectedStudies(new Set());
                            setLines([]);
                          }
                        }}
                        className="rounded border-gray-300"
                      />
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Code</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Study Name</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Modality</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Dept</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">TAT</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Rate</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Qty</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {isLoading ? (
                    <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-500">Loading studies...</td></tr>
                  ) : filteredStudies.length === 0 ? (
                    <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-500">No studies found</td></tr>
                  ) : (
                    filteredStudies.map((study) => (
                      <tr
                        key={study.itemId}
                        onClick={() => toggleStudy(study.itemId)}
                        className={`cursor-pointer transition-colors ${
                          selectedStudies.has(study.itemId) ? "bg-blue-50 hover:bg-blue-100" : "hover:bg-gray-50"
                        }`}
                      >
                        <td className="px-4 py-3">
                          <input
                            type="checkbox"
                            checked={selectedStudies.has(study.itemId)}
                            onChange={() => toggleStudy(study.itemId)}
                            className="rounded border-gray-300"
                            onClick={(e) => e.stopPropagation()}
                          />
                        </td>
                        <td className="px-4 py-3 font-mono text-sm text-gray-900">{study.itemCode}</td>
                        <td className="px-4 py-3 text-sm text-gray-900">{study.itemName}</td>
                        <td className="px-4 py-3">
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                            {study.modality}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600">{study.department}</td>
                        <td className="px-4 py-3 text-sm text-gray-600 text-right font-mono">{study.tat}h</td>
                        <td className="px-4 py-3 text-sm text-gray-900 text-right font-mono">₹{study.rate.toFixed(2)}</td>
                        <td className="px-4 py-3 text-right">
                          {selectedStudies.has(study.itemId) && (
                            <input
                              type="number"
                              min={1}
                              value={lines.find((l) => l.itemId === study.itemId)?.qty ?? 1}
                              onChange={(e) => updateLineQty(study.itemId, Number(e.target.value))}
                              onClick={(e) => e.stopPropagation()}
                              className="w-16 px-2 py-1 border border-gray-300 rounded text-sm text-right"
                            />
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {errors.lines && <p className="text-xs text-red-600 mt-1">{errors.lines}</p>}
          </div>

          <div className="flex items-center gap-2 pt-2">
            <button
              type="button"
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm"
              onClick={handleSubmit}
              disabled={selectedStudies.size === 0 || createOrder.isPending}
            >
              {createOrder.isPending ? "Booking..." : `Book ${selectedStudies.size} Study${selectedStudies.size !== 1 ? "ies" : ""}`}
            </button>
            <button
              type="button"
              className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 text-sm"
              onClick={() => { setShowForm(false); setSelectedStudies(new Set()); setLines([]); }}
            >
              Cancel
            </button>
          </div>

          {createOrder.isError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-800">Failed to create order: {(createOrder.error as Error).message}</p>
            </div>
          )}
        </div>
      )}

      {/* Existing Orders with Reschedule/Retest */}
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
          <h3 className="text-sm font-medium text-gray-900">Existing Orders ({orders.length})</h3>
        </div>
        {orders.length === 0 ? (
          <div className="p-8 text-center text-gray-500">No orders found</div>
        ) : (
          <div className="divide-y divide-gray-200">
            {orders.map((order) => (
              <RadOrderRow
                key={order.radOrderId}
                order={order}
                onReschedule={handleReschedule}
                onRetest={handleRetest}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

interface RadOrderRowProps {
  order: RadOrder;
  onReschedule: (orderId: number, lineNo: number, rescheduledTo: string) => Promise<void>;
  onRetest: (orderId: number, lineNo: number, reason: string) => Promise<void>;
}

function RadOrderRow({ order, onReschedule, onRetest }: RadOrderRowProps) {
  const [showActions, setShowActions] = useState(false);
  const [rescheduleDate, setRescheduleDate] = useState("");
  const [retestReason, setRetestReason] = useState("");
  const [activeLine, setActiveLine] = useState<number | null>(null);

  const statusInfo = order.status === 1 ? { label: "SCHEDULED", color: "bg-blue-100 text-blue-800" }
    : order.status === 2 ? { label: "IN PROGRESS", color: "bg-yellow-100 text-yellow-800" }
    : order.status === 3 ? { label: "COMPLETED", color: "bg-green-100 text-green-800" }
    : { label: "CANCELLED", color: "bg-red-100 text-red-800" };

  return (
    <div className="p-4 hover:bg-gray-50 transition-colors">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-sm font-mono text-gray-500">#{order.radOrderId}</span>
          <span className="text-sm font-medium text-gray-900">{order.modality}</span>
          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${statusInfo.color}`}>
            {statusInfo.label}
          </span>
          {order.priority === 2 && (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-orange-100 text-orange-800">
              URGENT
            </span>
          )}
        </div>
        <button
          type="button"
          className="text-xs text-blue-600 hover:text-blue-800"
          onClick={() => setShowActions(!showActions)}
        >
          {showActions ? "Hide Actions" : "Actions"}
        </button>
      </div>

      {order.details && order.details.length > 0 && (
        <div className="mt-2 text-xs text-gray-500">
          {order.details.map((d) => (
            <span key={d.lineNo} className="mr-3">
              Line {d.lineNo}: Item #{d.itemId} (×{d.qty})
              {d.retestRequested && <span className="text-orange-600 ml-1">[Retest]</span>}
              {d.rescheduledTo && <span className="text-blue-600 ml-1">[Rescheduled]</span>}
            </span>
          ))}
        </div>
      )}

      {showActions && order.status !== 3 && order.status !== 4 && (
        <div className="mt-3 p-3 bg-gray-50 rounded-md space-y-3">
          {/* Reschedule */}
          <div className="flex items-center gap-2">
            <input
              type="datetime-local"
              value={rescheduleDate}
              onChange={(e) => setRescheduleDate(e.target.value)}
              className="px-2 py-1 border border-gray-300 rounded text-xs"
            />
            <button
              type="button"
              className="px-3 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700"
              disabled={!rescheduleDate || activeLine === null}
              onClick={() => {
                if (activeLine !== null && rescheduleDate) {
                  onReschedule(order.radOrderId, activeLine, rescheduleDate);
                  setRescheduleDate("");
                }
              }}
            >
              Reschedule Line {activeLine ?? "?"}
            </button>
            <select
              value={activeLine ?? ""}
              onChange={(e) => setActiveLine(Number(e.target.value))}
              className="px-2 py-1 border border-gray-300 rounded text-xs"
            >
              <option value="">Select line...</option>
              {order.details?.map((d) => (
                <option key={d.lineNo} value={d.lineNo}>Line {d.lineNo}</option>
              ))}
            </select>
          </div>

          {/* Retest */}
          <div className="flex items-center gap-2">
            <input
              type="text"
              placeholder="Retest reason..."
              value={retestReason}
              onChange={(e) => setRetestReason(e.target.value)}
              className="flex-1 px-2 py-1 border border-gray-300 rounded text-xs"
            />
            <button
              type="button"
              className="px-3 py-1 bg-orange-600 text-white rounded text-xs hover:bg-orange-700"
              disabled={!retestReason || activeLine === null}
              onClick={() => {
                if (activeLine !== null && retestReason) {
                  onRetest(order.radOrderId, activeLine, retestReason);
                  setRetestReason("");
                }
              }}
            >
              Request Retest
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
