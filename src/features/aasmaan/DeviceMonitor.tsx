import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useDevices,
  useMonitors,
  useRecordMonitor,
  useCreateDevice,
  useDeleteDevice,
} from "./queries";
import { monitorSchema, deviceSchema, type MonitorValues, type DeviceValues } from "./zodSchema";

const DEVICE_TYPE_ICONS: Record<string, string> = {
  BP_MONITOR: "🩸",
  GLUCOMETER: "🩸",
  PULSE_OXIMETER: "💓",
  THERMOMETER: "🌡️",
  WEIGHT_SCALE: "⚖️",
  ECG: "📈",
  OTHER: "📱",
};

const DEVICE_STATUS_COLORS: Record<string, string> = {
  ACTIVE: "bg-green-500",
  INACTIVE: "bg-gray-400",
  MAINTENANCE: "bg-yellow-500",
};

interface DeviceMonitorProps {
  selectedCaseId?: number;
}

/**
 * DeviceMonitor — Linear-style IoT monitoring dashboard.
 * Top: metric recording + real-time values. Bottom: device registry with status.
 */
export function DeviceMonitor({ selectedCaseId }: DeviceMonitorProps) {
  const [activeTab, setActiveTab] = useState<"metrics" | "devices">("metrics");

  const { data: devices = [], isLoading: devicesLoading } = useDevices();
  const { data: monitors = [], isLoading: monitorsLoading } = useMonitors(selectedCaseId);
  const recordMonitor = useRecordMonitor();
  const createDevice = useCreateDevice();
  const deleteDevice = useDeleteDevice();

  const {
    register: registerMetric,
    handleSubmit: handleSubmitMetric,
    reset: resetMetric,
    formState: { errors: metricErrors },
  } = useForm<MonitorValues>({
    resolver: zodResolver(monitorSchema),
    defaultValues: {
      caseId: selectedCaseId ?? 0,
      metric: "",
      value: 0,
    },
  });

  const {
    register: registerDevice,
    handleSubmit: handleSubmitDevice,
    reset: resetDevice,
    formState: { errors: deviceErrors },
  } = useForm<DeviceValues>({
    resolver: zodResolver(deviceSchema),
    defaultValues: {
      deviceName: "",
      deviceType: "OTHER",
      serialNo: "",
      status: "ACTIVE",
    },
  });

  const handleRecordMetric = async (values: MonitorValues) => {
    try {
      await recordMonitor.mutateAsync(values);
      resetMetric({ ...values, metric: "", value: 0 });
    } catch (err) {
      console.error("Failed to record metric:", err);
    }
  };

  const handleRegisterDevice = async (values: DeviceValues) => {
    try {
      await createDevice.mutateAsync(values);
      resetDevice({ deviceName: "", deviceType: "OTHER", serialNo: "", status: "ACTIVE" });
    } catch (err) {
      console.error("Failed to register device:", err);
    }
  };

  const handleDeleteDevice = async (id: number) => {
    if (!confirm("Remove this device?")) return;
    try {
      await deleteDevice.mutateAsync(id);
    } catch (err) {
      console.error("Delete failed:", err);
    }
  };

  const activeDevices = devices.filter((d) => d.status === "ACTIVE").length;
  const latestMetrics = monitors.slice(0, 10);

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">IoT Monitor</h2>
          <p className="text-sm text-gray-500">Device registry and real-time metrics</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-500">
            {activeDevices} active device{activeDevices !== 1 ? "s" : ""}
          </span>
        </div>
      </header>

      {/* Summary cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-blue-700 font-medium">Total Devices</p>
              <p className="text-3xl font-bold text-blue-900 mt-1">{devices.length}</p>
            </div>
            <div className="text-4xl opacity-50">📱</div>
          </div>
        </div>
        <div className="bg-gradient-to-br from-green-50 to-green-100 border border-green-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-green-700 font-medium">Active</p>
              <p className="text-3xl font-bold text-green-900 mt-1">{activeDevices}</p>
            </div>
            <div className="text-4xl opacity-50">✓</div>
          </div>
        </div>
        <div className="bg-gradient-to-br from-purple-50 to-purple-100 border border-purple-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-purple-700 font-medium">Metrics Recorded</p>
              <p className="text-3xl font-bold text-purple-900 mt-1">{monitors.length}</p>
            </div>
            <div className="text-4xl opacity-50">📊</div>
          </div>
        </div>
        <div className="bg-gradient-to-br from-yellow-50 to-yellow-100 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-yellow-700 font-medium">In Maintenance</p>
              <p className="text-3xl font-bold text-yellow-900 mt-1">
                {devices.filter((d) => d.status === "MAINTENANCE").length}
              </p>
            </div>
            <div className="text-4xl opacity-50">🔧</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-4" aria-label="Device Monitor Tabs">
          <button
            type="button"
            className={`pb-2 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "metrics"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
            onClick={() => setActiveTab("metrics")}
          >
            Metrics
          </button>
          <button
            type="button"
            className={`pb-2 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "devices"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
            onClick={() => setActiveTab("devices")}
          >
            Devices
          </button>
        </nav>
      </div>

      {activeTab === "metrics" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Metric recording form */}
          <div className="border border-gray-200 rounded-lg p-4">
            <h3 className="text-sm font-medium text-gray-900 mb-4">Record Metric</h3>
            <form onSubmit={handleSubmitMetric(handleRecordMetric)} className="space-y-4">
              <div>
                <label htmlFor="metric-caseId" className="block text-xs font-medium text-gray-700 mb-1">
                  Case ID *
                </label>
                <input
                  id="metric-caseId"
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...registerMetric("caseId")}
                />
                {metricErrors.caseId && (
                  <p className="text-xs text-red-600 mt-1">{metricErrors.caseId.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="metric-name" className="block text-xs font-medium text-gray-700 mb-1">
                  Metric *
                </label>
                <input
                  id="metric-name"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  placeholder="e.g. BP_SYSTOLIC, SPO2"
                  {...registerMetric("metric")}
                />
                {metricErrors.metric && (
                  <p className="text-xs text-red-600 mt-1">{metricErrors.metric.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="metric-value" className="block text-xs font-medium text-gray-700 mb-1">
                  Value *
                </label>
                <input
                  id="metric-value"
                  type="number"
                  step="any"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...registerMetric("value")}
                />
                {metricErrors.value && (
                  <p className="text-xs text-red-600 mt-1">{metricErrors.value.message}</p>
                )}
              </div>
              <button
                type="submit"
                className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm"
                disabled={recordMonitor.isPending}
              >
                {recordMonitor.isPending ? "Recording..." : "Record Metric"}
              </button>
              {recordMonitor.isError && (
                <p className="text-xs text-red-600">Failed to record metric.</p>
              )}
            </form>
          </div>

          {/* Latest metrics */}
          <div className="lg:col-span-2 border border-gray-200 rounded-lg overflow-hidden">
            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
              <h3 className="text-sm font-medium text-gray-900">Latest Readings</h3>
            </div>
            {monitorsLoading ? (
              <div className="p-8 text-center text-gray-500">Loading metrics...</div>
            ) : latestMetrics.length === 0 ? (
              <div className="p-8 text-center text-gray-500">No metrics recorded</div>
            ) : (
              <div className="divide-y divide-gray-200">
                {latestMetrics.map((m) => (
                  <div key={m.monitorId} className="p-3 flex items-center justify-between hover:bg-gray-50">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-xs font-medium text-blue-700">
                        📊
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-900">{m.metric}</p>
                        <p className="text-xs text-gray-500">Case #{m.caseId}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-gray-900 font-mono">{m.value}</p>
                      <p className="text-xs text-gray-500">{m.recordedAt ?? "—"}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === "devices" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Device registration form */}
          <div className="border border-gray-200 rounded-lg p-4">
            <h3 className="text-sm font-medium text-gray-900 mb-4">Register Device</h3>
            <form onSubmit={handleSubmitDevice(handleRegisterDevice)} className="space-y-4">
              <div>
                <label htmlFor="device-name" className="block text-xs font-medium text-gray-700 mb-1">
                  Device Name *
                </label>
                <input
                  id="device-name"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...registerDevice("deviceName")}
                />
                {deviceErrors.deviceName && (
                  <p className="text-xs text-red-600 mt-1">{deviceErrors.deviceName.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="device-type" className="block text-xs font-medium text-gray-700 mb-1">
                  Device Type
                </label>
                <select
                  id="device-type"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...registerDevice("deviceType")}
                >
                  <option value="BP_MONITOR">BP Monitor</option>
                  <option value="GLUCOMETER">Glucometer</option>
                  <option value="PULSE_OXIMETER">Pulse Oximeter</option>
                  <option value="THERMOMETER">Thermometer</option>
                  <option value="WEIGHT_SCALE">Weight Scale</option>
                  <option value="ECG">ECG</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div>
                <label htmlFor="device-serial" className="block text-xs font-medium text-gray-700 mb-1">
                  Serial No *
                </label>
                <input
                  id="device-serial"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...registerDevice("serialNo")}
                />
                {deviceErrors.serialNo && (
                  <p className="text-xs text-red-600 mt-1">{deviceErrors.serialNo.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="device-status" className="block text-xs font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  id="device-status"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...registerDevice("status")}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="MAINTENANCE">Maintenance</option>
                </select>
              </div>
              <button
                type="submit"
                className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm"
                disabled={createDevice.isPending}
              >
                {createDevice.isPending ? "Registering..." : "Register Device"}
              </button>
              {createDevice.isError && (
                <p className="text-xs text-red-600">Failed to register device.</p>
              )}
            </form>
          </div>

          {/* Device list */}
          <div className="lg:col-span-2 border border-gray-200 rounded-lg overflow-hidden">
            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
              <h3 className="text-sm font-medium text-gray-900">Registered Devices</h3>
            </div>
            {devicesLoading ? (
              <div className="p-8 text-center text-gray-500">Loading devices...</div>
            ) : devices.length === 0 ? (
              <div className="p-8 text-center text-gray-500">No devices registered</div>
            ) : (
              <div className="divide-y divide-gray-200">
                {devices.map((d) => (
                  <div key={d.deviceId} className="p-4 hover:bg-gray-50">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <span className="text-lg">{DEVICE_TYPE_ICONS[d.deviceType] ?? "📱"}</span>
                        <div>
                          <p className="text-sm font-medium text-gray-900">{d.deviceName}</p>
                          <p className="text-xs text-gray-500 font-mono">{d.serialNo}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="text-right">
                          <p className="text-xs text-gray-500">{d.deviceType.replace(/_/g, " ")}</p>
                          <p className="text-xs text-gray-500">Last seen: {d.lastSeenAt ?? "—"}</p>
                        </div>
                        <div className={`w-3 h-3 rounded-full ${DEVICE_STATUS_COLORS[d.status] ?? "bg-gray-400"}`} />
                        <button
                          type="button"
                          className="text-xs text-red-600 hover:text-red-800"
                          onClick={() => handleDeleteDevice(d.deviceId)}
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
