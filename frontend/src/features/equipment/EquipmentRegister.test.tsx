import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EquipmentRegister } from "./EquipmentRegister";

/**
 * Component test for EquipmentRegister — verifies the asset register form,
 * table rendering, and the register/update/transfer mutations.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useEquipmentList: vi.fn(),
    useRegisterEquipment: vi.fn(),
    useUpdateEquipment: vi.fn(),
    useTransferEquipment: vi.fn(),
  };
});

const queries = await import("./queries");
const useEquipmentList = queries.useEquipmentList as unknown as ReturnType<typeof vi.fn>;
const useRegisterEquipment = queries.useRegisterEquipment as unknown as ReturnType<typeof vi.fn>;
const useUpdateEquipment = queries.useUpdateEquipment as unknown as ReturnType<typeof vi.fn>;
const useTransferEquipment = queries.useTransferEquipment as unknown as ReturnType<typeof vi.fn>;

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <EquipmentRegister />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  useEquipmentList.mockReturnValue({ data: [], isLoading: false, error: null });
  useRegisterEquipment.mockReturnValue({ mutate: vi.fn(), isPending: false });
  useUpdateEquipment.mockReturnValue({ mutate: vi.fn(), isPending: false });
  useTransferEquipment.mockReturnValue({ mutate: vi.fn(), isPending: false });
});

describe("EquipmentRegister", () => {
  it("renders empty state when no equipment", () => {
    renderPage();
    expect(screen.getByText(/No equipment registered/)).toBeInTheDocument();
  });

  it("renders equipment list with working status badges", () => {
    useEquipmentList.mockReturnValue({
      data: [
        {
          id: 1,
          itemId: 100,
          accessionNo: "ACC-001",
          serialNo: "SN-123",
          model: "Model X",
          make: "Acme",
          location: "ICU",
          workingStatus: 1,
        },
        {
          id: 2,
          itemId: 101,
          accessionNo: "ACC-002",
          workingStatus: 3,
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    expect(screen.getByText("ACC-001")).toBeInTheDocument();
    expect(screen.getByText("ACC-002")).toBeInTheDocument();
    expect(screen.getByText("Working")).toBeInTheDocument();
    expect(screen.getByText("Under Repair")).toBeInTheDocument();
  });

  it("shows loading state", () => {
    useEquipmentList.mockReturnValue({ data: [], isLoading: true, error: null });
    renderPage();
    expect(screen.getByText(/Loading equipment/)).toBeInTheDocument();
  });

  it("shows error state", () => {
    useEquipmentList.mockReturnValue({
      data: [],
      isLoading: false,
      error: new Error("Network error"),
    });
    renderPage();
    expect(screen.getByText(/Error loading equipment/)).toBeInTheDocument();
  });

  it("opens the register form on button click", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /Register Equipment/ }));
    expect(screen.getByText(/Register New Equipment/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Item ID/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Accession No./)).toBeInTheDocument();
  });

  it("validates required fields on submit", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn();
    useRegisterEquipment.mockReturnValue({ mutate: mockMutate, isPending: false });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Register Equipment/ }));

    // Submit without filling required fields
    await user.click(screen.getByRole("button", { name: /Register$/ }));

    expect(screen.getByText(/Item is required/)).toBeInTheDocument();
    expect(screen.getByText(/Accession number required/)).toBeInTheDocument();
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it("submits valid form data", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn((_payload, opts) => opts?.onSuccess?.());
    useRegisterEquipment.mockReturnValue({ mutate: mockMutate, isPending: false });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Register Equipment/ }));

    await user.type(screen.getByLabelText(/Item ID/), "100");
    await user.type(screen.getByLabelText(/Accession No./), "ACC-001");
    await user.type(screen.getByLabelText(/Serial No./), "SN-123");
    await user.type(screen.getByLabelText(/Model/), "Model X");

    await user.click(screen.getByRole("button", { name: /Register$/ }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledTimes(1);
    });
    const [payload] = mockMutate.mock.calls[0];
    expect(payload).toMatchObject({
      itemId: 100,
      accessionNo: "ACC-001",
      serialNo: "SN-123",
      model: "Model X",
    });
  });

  it("opens edit form with pre-filled data", async () => {
    const user = userEvent.setup();
    useEquipmentList.mockReturnValue({
      data: [
        {
          id: 1,
          itemId: 100,
          accessionNo: "ACC-001",
          serialNo: "SN-123",
          model: "Model X",
          make: "Acme",
          location: "ICU",
          workingStatus: 1,
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Edit/ }));

    expect(screen.getByText(/Edit Equipment/)).toBeInTheDocument();
    expect(screen.getByDisplayValue("ACC-001")).toBeInTheDocument();
    expect(screen.getByDisplayValue("SN-123")).toBeInTheDocument();
  });

  it("opens transfer form and submits", async () => {
    const user = userEvent.setup();
    const mockTransfer = vi.fn((_payload, opts) => opts?.onSuccess?.());
    useTransferEquipment.mockReturnValue({ mutate: mockTransfer, isPending: false });

    useEquipmentList.mockReturnValue({
      data: [
        {
          id: 1,
          itemId: 100,
          accessionNo: "ACC-001",
          workingStatus: 1,
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Transfer/ }));

    expect(screen.getByText(/Transfer Equipment #1/)).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Target Department ID/), "5");
    await user.type(screen.getByLabelText(/Reason/), "Moving to ICU");

    await user.click(screen.getByRole("button", { name: /Confirm Transfer/ }));

    await waitFor(() => {
      expect(mockTransfer).toHaveBeenCalledTimes(1);
    });
    const [{ id, payload }] = mockTransfer.mock.calls[0];
    expect(id).toBe(1);
    expect(payload).toMatchObject({
      toDepartmentId: 5,
      reason: "Moving to ICU",
    });
  });
});
