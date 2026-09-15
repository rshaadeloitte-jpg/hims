import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useETickets, useCreateETicket } from "./queries";
import { eTicketSchema, type ETicketValues } from "./zodSchema";

const PRIORITY_MAP: Record<string, { label: string; className: string }> = {
  LOW: { label: "Low", className: "bg-gray-100 text-gray-700" },
  MEDIUM: { label: "Medium", className: "bg-blue-100 text-blue-800" },
  HIGH: { label: "High", className: "bg-orange-100 text-orange-800" },
  CRITICAL: { label: "Critical", className: "bg-red-100 text-red-800" },
};

const STATUS_BADGES: Record<string, string> = {
  OPEN: "bg-blue-100 text-blue-800",
  IN_PROGRESS: "bg-yellow-100 text-yellow-800",
  RESOLVED: "bg-green-100 text-green-800",
  CLOSED: "bg-gray-100 text-gray-700",
};

interface ETicketFormProps {
  defaultCaseId?: number;
  onTicketCreated?: (ticketId: number) => void;
}

/**
 * ETicketForm — Linear-style e-ticket creation + ticket list.
 * Top: create form with priority selector. Bottom: ticket list with status.
 */
export function ETicketForm({ defaultCaseId, onTicketCreated }: ETicketFormProps) {
  const [showList, setShowList] = useState(true);

  const { data: tickets = [], isLoading } = useETickets();
  const createETicket = useCreateETicket();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ETicketValues>({
    resolver: zodResolver(eTicketSchema),
    defaultValues: {
      caseId: defaultCaseId,
      subject: "",
      priority: "MEDIUM",
      status: "OPEN",
    },
  });

  const onSubmit = async (values: ETicketValues) => {
    try {
      const ticket = await createETicket.mutateAsync(values);
      reset({ caseId: defaultCaseId, subject: "", priority: "MEDIUM", status: "OPEN" });
      onTicketCreated?.(ticket.ticketId);
    } catch (err) {
      console.error("Failed to create e-ticket:", err);
    }
  };

  const sortedTickets = [...tickets].sort((a, b) => {
    const priorityOrder = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
    return (priorityOrder[a.priority as keyof typeof priorityOrder] ?? 4) - (priorityOrder[b.priority as keyof typeof priorityOrder] ?? 4);
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">E-Tickets</h2>
          <p className="text-sm text-gray-500">Create and track support tickets</p>
        </div>
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          onClick={() => setShowList(!showList)}
        >
          {showList ? "New Ticket" : "View All"}
        </button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Create form */}
        <div className="border border-gray-200 rounded-lg p-4">
          <h3 className="text-sm font-medium text-gray-900 mb-4">Create E-Ticket</h3>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label htmlFor="et-caseId" className="block text-xs font-medium text-gray-700 mb-1">
                Case ID
              </label>
              <input
                id="et-caseId"
                type="number"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                {...register("caseId")}
              />
              {errors.caseId && (
                <p className="text-xs text-red-600 mt-1">{errors.caseId.message}</p>
              )}
            </div>

            <div>
              <label htmlFor="et-subject" className="block text-xs font-medium text-gray-700 mb-1">
                Subject *
              </label>
              <input
                id="et-subject"
                type="text"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                placeholder="Brief description of the issue"
                {...register("subject")}
              />
              {errors.subject && (
                <p className="text-xs text-red-600 mt-1">{errors.subject.message}</p>
              )}
            </div>

            <div>
              <label htmlFor="et-priority" className="block text-xs font-medium text-gray-700 mb-1">
                Priority
              </label>
              <select
                id="et-priority"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                {...register("priority")}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div>
              <label htmlFor="et-status" className="block text-xs font-medium text-gray-700 mb-1">
                Status
              </label>
              <select
                id="et-status"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                {...register("status")}
              >
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>

            <button
              type="submit"
              className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm"
              disabled={createETicket.isPending}
            >
              {createETicket.isPending ? "Creating..." : "Create Ticket"}
            </button>

            {createETicket.isError && (
              <div className="p-2 bg-red-50 border border-red-200 rounded text-xs text-red-700">
                Failed to create ticket. Please try again.
              </div>
            )}
          </form>
        </div>

        {/* Ticket list */}
        <div className="border border-gray-200 rounded-lg overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <h3 className="text-sm font-medium text-gray-900">
              Recent Tickets ({tickets.length})
            </h3>
          </div>
          {isLoading ? (
            <div className="p-8 text-center text-gray-500">Loading tickets...</div>
          ) : sortedTickets.length === 0 ? (
            <div className="p-8 text-center text-gray-500">No tickets yet</div>
          ) : (
            <div className="divide-y divide-gray-200 max-h-96 overflow-y-auto">
              {sortedTickets.map((t) => {
                const priorityInfo = PRIORITY_MAP[t.priority] ?? PRIORITY_MAP.MEDIUM;
                const statusClass = STATUS_BADGES[t.status] ?? "bg-gray-100 text-gray-700";
                return (
                  <div key={t.ticketId} className="p-3 hover:bg-gray-50">
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono text-gray-400">#{t.ticketId}</span>
                        <span className="text-sm font-medium text-gray-900 truncate max-w-48">
                          {t.subject}
                        </span>
                      </div>
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${priorityInfo.className}`}>
                        {priorityInfo.label}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-gray-500">
                      <div className="flex items-center gap-2">
                        <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium ${statusClass}`}>
                          {t.status}
                        </span>
                        {t.caseId && <span>Case #{t.caseId}</span>}
                      </div>
                      <span>{t.createdAt ?? "—"}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
