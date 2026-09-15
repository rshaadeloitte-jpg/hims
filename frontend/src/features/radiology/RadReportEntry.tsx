import { useState, useCallback } from "react";
import { useRadOrder, useReportRadOrder, useRadTemplates } from "./queries";
import { radReportSchema } from "./zodSchema";
import { statusLabel, statusColor } from "./types";

interface RadReportEntryProps {
  orderId: number;
  onCompleted?: () => void;
}

/**
 * M16 RadReportEntry — Template-based report with body sections.
 * Loads order, shows template selector, fills report sections, submits.
 */
export function RadReportEntry({ orderId, onCompleted }: RadReportEntryProps) {
  const { data: order, isLoading } = useRadOrder(orderId);
  const { data: templates = [] } = useRadTemplates(order?.modality);
  const reportOrder = useReportRadOrder();

  const [reportText, setReportText] = useState("");
  const [reportedBy, setReportedBy] = useState("");
  const [selectedTemplate, setSelectedTemplate] = useState<number | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [showTemplatePanel, setShowTemplatePanel] = useState(false);

  const applyTemplate = useCallback(
    (templateId: number) => {
      const template = templates.find((t) => t.id === templateId);
      if (template) {
        setReportText(template.bodyTemplate);
        setSelectedTemplate(templateId);
        setShowTemplatePanel(false);
      }
    },
    [templates],
  );

  const handleSubmit = () => {
    const result = radReportSchema.safeParse({ reportText, reportedBy });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0]] = e.message;
      });
      setErrors(fieldErrors);
      return;
    }
    reportOrder.mutate(
      { id: orderId, reportText, reportedBy },
      {
        onSuccess: () => {
          setReportText("");
          setReportedBy("");
          setSelectedTemplate(null);
          setErrors({});
          onCompleted?.();
        },
      },
    );
  };

  if (isLoading) return <div className="p-4 text-center text-gray-500">Loading order...</div>;
  if (!order) return <div className="p-4 text-center text-red-600">Order not found</div>;

  const canReport = order.status === 2; // IN_PROGRESS

  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Report Entry</h2>
          <p className="text-sm text-gray-500">
            Order #{orderId} — {order.modality} —{" "}
            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${statusColor(order.status)}`}>
              {statusLabel(order.status)}
            </span>
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 text-sm"
            onClick={() => setShowTemplatePanel(!showTemplatePanel)}
          >
            {showTemplatePanel ? "Hide Templates" : "Templates"}
          </button>
          <button
            type="button"
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors text-sm"
            onClick={handleSubmit}
            disabled={!canReport || reportOrder.isPending}
          >
            {reportOrder.isPending ? "Submitting..." : "Submit Report"}
          </button>
        </div>
      </header>

      {!canReport && (
        <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-md">
          <p className="text-sm text-yellow-800">
            Reporting requires the order to be IN_PROGRESS. Current status: {statusLabel(order.status)}.
          </p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Report Editor */}
        <div className="lg:col-span-2 space-y-4">
          {/* Order Lines Summary */}
          {order.details && order.details.length > 0 && (
            <div className="border border-gray-200 rounded-lg p-4">
              <h3 className="text-sm font-medium text-gray-900 mb-2">Studies</h3>
              <div className="flex flex-wrap gap-2">
                {order.details.map((d) => (
                  <span
                    key={d.lineNo}
                    className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700"
                  >
                    Line {d.lineNo}: Item #{d.itemId} (×{d.qty})
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Report Body Sections */}
          <div className="border border-gray-200 rounded-lg p-4 space-y-4">
            <h3 className="text-sm font-medium text-gray-900">Report Body</h3>

            <div>
              <label htmlFor="report-text" className="block text-xs font-medium text-gray-700 mb-1">
                Report Text *
              </label>
              <textarea
                id="report-text"
                rows={12}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm font-mono"
                placeholder="Enter findings, impressions, and recommendations..."
                value={reportText}
                onChange={(e) => setReportText(e.target.value)}
                disabled={!canReport}
              />
              {errors.reportText && <p className="text-xs text-red-600 mt-1">{errors.reportText}</p>}
            </div>

            <div>
              <label htmlFor="reported-by" className="block text-xs font-medium text-gray-700 mb-1">
                Reported By *
              </label>
              <input
                id="reported-by"
                type="text"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                placeholder="Radiologist name"
                value={reportedBy}
                onChange={(e) => setReportedBy(e.target.value)}
                disabled={!canReport}
              />
              {errors.reportedBy && <p className="text-xs text-red-600 mt-1">{errors.reportedBy}</p>}
            </div>
          </div>

          {reportOrder.isError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-800">Failed to submit report: {(reportOrder.error as Error).message}</p>
            </div>
          )}
        </div>

        {/* Template Panel */}
        {showTemplatePanel && (
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
              <h3 className="text-sm font-medium text-gray-900">
                Templates {order.modality && `(${order.modality})`}
              </h3>
            </div>
            {templates.length === 0 ? (
              <div className="p-4 text-center text-gray-500 text-sm">No templates available</div>
            ) : (
              <div className="divide-y divide-gray-200 max-h-96 overflow-y-auto">
                {templates.map((t) => (
                  <div
                    key={t.id}
                    className={`p-3 cursor-pointer transition-colors ${
                      selectedTemplate === t.id ? "bg-blue-50" : "hover:bg-gray-50"
                    }`}
                    onClick={() => applyTemplate(t.id)}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-sm font-medium text-gray-900">{t.templateCode}</span>
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                        {t.modality}
                      </span>
                    </div>
                    {t.description && (
                      <p className="text-xs text-gray-500 line-clamp-2">{t.description}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-1 font-mono line-clamp-2">
                      {t.bodyTemplate.substring(0, 100)}...
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
