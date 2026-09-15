import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DeviceMonitor } from "./DeviceMonitor";

const mockDevices = [
  { deviceId: 1, deviceName: "BP Monitor A", deviceType: "BP_MONITOR", serialNo: "BP001", status: "ACTIVE", lastSeenAt: "2024-01-15T10:30:00" },
  { deviceId: 2, deviceName: "Glucometer X", deviceType: "GLUCOMETER", serialNo: "GL002", status: "MAINTENANCE", lastSeenAt: "2024-01-14T08:00:00" },
];

const mockMonitors = [
  { monitorId: 1, caseId: 1, metric: "BP_SYSTOLIC", value: 120, recordedAt: "2024-01-15T10:30:00" },
];

let mockDevicesData = mockDevices;
let mockMonitorsData = mockMonitors;
let mockDevicesLoading = false;
let mockMonitorsLoading = false;

const mockMutateAsync = vi.fn();

vi.mock("./queries", () => ({
  useDevices: () => ({ data: mockDevicesData, isLoading: mockDevicesLoading }),
  useMonitors: () => ({ data: mockMonitorsData, isLoading: mockMonitorsLoading }),
  useRecordMonitor: () => ({ mutateAsync: mockMutateAsync, isPending: false, isError: false }),
  useCreateDevice: () => ({ mutateAsync: vi.fn(), isPending: false, isError: false }),
  useDeleteDevice: () => ({ mutateAsync: vi.fn(), isPending: false, isError: false }),
}));

function renderDeviceMonitor() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <DeviceMonitor />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockDevicesData = mockDevices;
  mockMonitorsData = mockMonitors;
  mockDevicesLoading = false;
  mockMonitorsLoading = false;
  mockMutateAsync.mockResolvedValue({ monitorId: 100 });
});

describe("DeviceMonitor", () => {
  it("renders the IoT monitor header", () => {
    renderDeviceMonitor();
    expect(screen.getByText("IoT Monitor")).toBeInTheDocument();
  });

  it("renders summary cards", () => {
    renderDeviceMonitor();
    expect(screen.getByText("Total Devices")).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(screen.getByText("Metrics Recorded")).toBeInTheDocument();
    expect(screen.getByText("In Maintenance")).toBeInTheDocument();
  });

  it("shows total device count in summary", () => {
    renderDeviceMonitor();
    expect(screen.getByText("Total Devices")).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("renders tab navigation", () => {
    renderDeviceMonitor();
    expect(screen.getByText("Metrics")).toBeInTheDocument();
    expect(screen.getByText("Devices")).toBeInTheDocument();
  });

  it("renders metrics tab by default", () => {
    renderDeviceMonitor();
    expect(screen.getByText("Record Metric")).toBeInTheDocument();
    expect(screen.getByText("Latest Readings")).toBeInTheDocument();
  });

  it("shows metric data", () => {
    renderDeviceMonitor();
    expect(screen.getByText("BP_SYSTOLIC")).toBeInTheDocument();
    expect(screen.getByText("120")).toBeInTheDocument();
  });

  it("switches to devices tab", async () => {
    renderDeviceMonitor();
    fireEvent.click(screen.getByText("Devices"));
    await waitFor(() => {
      expect(screen.getByText("Register Device")).toBeInTheDocument();
      expect(screen.getByText("Registered Devices")).toBeInTheDocument();
    });
  });

  it("shows device data in devices tab", async () => {
    renderDeviceMonitor();
    fireEvent.click(screen.getByText("Devices"));
    await waitFor(() => {
      expect(screen.getByText("BP Monitor A")).toBeInTheDocument();
      expect(screen.getByText("Glucometer X")).toBeInTheDocument();
    });
  });

  it("renders record metric form", () => {
    renderDeviceMonitor();
    expect(screen.getByLabelText("Case ID *")).toBeInTheDocument();
    expect(screen.getByLabelText("Metric *")).toBeInTheDocument();
    expect(screen.getByLabelText("Value *")).toBeInTheDocument();
  });

  it("renders device registration form", async () => {
    renderDeviceMonitor();
    fireEvent.click(screen.getByText("Devices"));
    await waitFor(() => {
      expect(screen.getByLabelText("Device Name *")).toBeInTheDocument();
      expect(screen.getByLabelText("Serial No *")).toBeInTheDocument();
    });
  });

  it("shows active device count in header", () => {
    renderDeviceMonitor();
    expect(screen.getByText("1 active device")).toBeInTheDocument();
  });
});
