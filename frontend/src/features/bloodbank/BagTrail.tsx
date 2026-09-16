import { useState } from "react";
import { Box, Text } from "@chakra-ui/react";
import { FormField, HimsSelect } from "../../components/ui";
import { useBloodBags, useBloodBag, useBagEvents } from "./queries";

const EVENT_META: Record<string, { icon: string; color: string }> = {
  COLLECTED: { icon: "🩸", color: "bg-red-100 text-red-700" },
  TESTED: { icon: "🧪", color: "bg-blue-100 text-blue-700" },
  SEPARATED: { icon: "🔬", color: "bg-purple-100 text-purple-700" },
  IRRADIATED: { icon: "☢️", color: "bg-yellow-100 text-yellow-700" },
  STORED: { icon: "🗄️", color: "bg-green-100 text-green-700" },
  RESERVED: { icon: "🔒", color: "bg-orange-100 text-orange-700" },
  ISSUED: { icon: "🚚", color: "bg-indigo-100 text-indigo-700" },
  TRANSFUSED: { icon: "💉", color: "bg-teal-100 text-teal-700" },
  RETURNED: { icon: "↩️", color: "bg-gray-100 text-gray-700" },
  QUARANTINED: { icon: "⚠️", color: "bg-amber-100 text-amber-700" },
  EXPIRED: { icon: "⏰", color: "bg-stone-100 text-stone-700" },
  DISCARDED: { icon: "🗑️", color: "bg-rose-100 text-rose-700" },
};

export function BagTrail() {
  const { data: bags = [], isLoading } = useBloodBags();
  const [selectedBagId, setSelectedBagId] = useState<number | null>(null);
  const selected = selectedBagId ? useBloodBag(selectedBagId) : null;
  const { data: events = [] } = useBagEvents(selectedBagId || 0);

  if (isLoading) return <p className="muted">Loading bag trail...</p>;

  const sorted = [...events].sort((a, b) => (a.occurredAt < b.occurredAt ? -1 : 1));

  return (
    <section className="page">
      <header className="page-header">
        <h1>Bag Trail</h1>
      </header>

      <div className="card">
        <h2>Select Bag</h2>
        <Box maxW="480px">
          <FormField label="Select Bag">
            <HimsSelect
              aria-label="Select Bag"
              value={selectedBagId ?? ""}
              onChange={(e) => setSelectedBagId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Select a bag...</option>
              {bags.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.bagNo} — {b.bloodGroup} / {b.component} [{b.status}]
                </option>
              ))}
            </HimsSelect>
          </FormField>
        </Box>
      </div>

      {selected?.data && (
        <div className="card">
          <h2>
            {selected.data.bagNo} — {selected.data.bloodGroup} / {selected.data.component}
          </h2>
          <Text fontSize="sm" color="gray.600" className="muted">
            Donor #{selected.data.donorId} · Collected {selected.data.collectionDate} · Expires {selected.data.expiryDate} ·{" "}
            <strong>{selected.data.status}</strong>
          </Text>
        </div>
      )}

      {sorted.length > 0 && (
        <div className="card">
          <h2>Timeline</h2>
          <ol className="relative border-s border-gray-200 dark:border-gray-700 ms-3.5">
            {sorted.map((ev) => {
              const meta = EVENT_META[ev.type] ?? { icon: "📍", color: "bg-gray-100 text-gray-700" };
              return (
                <li key={ev.id} className="mb-6 ms-4">
                  <span className="absolute flex items-center justify-center w-7 h-7 rounded-full -start-3.5 ring-1 ring-white bg-white">
                    <span className="text-sm">{meta.icon}</span>
                  </span>
                  <div className={`p-3 rounded-lg border ${meta.color}`}>
                    <div className="flex items-center justify-between">
                      <time className="text-xs font-medium">{new Date(ev.occurredAt).toLocaleString()}</time>
                      <span className="text-xs font-semibold">{ev.type}</span>
                    </div>
                    {ev.location && <p className="text-xs mt-1">Location: {ev.location}</p>}
                    {ev.performedBy && <p className="text-xs">By: {ev.performedBy}</p>}
                    {ev.note && <p className="text-xs italic mt-1">{ev.note}</p>}
                  </div>
                </li>
              );
            })}
          </ol>
        </div>
      )}

      {selectedBagId && sorted.length === 0 && (
        <div className="card"><p className="muted">No events for this bag.</p></div>
      )}
    </section>
  );
}
