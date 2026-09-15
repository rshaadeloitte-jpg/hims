import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { KioskSelfReg } from "./KioskSelfReg";

vi.mock("./queries", () => ({
  useSelfRegister: vi.fn(),
}));

const { useSelfRegister } = await import("./queries");
const mockMutate = vi.fn();

function renderComponent() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <KioskSelfReg />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useSelfRegister as ReturnType<typeof vi.fn>).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
  });
});

describe("KioskSelfReg", () => {
  it("renders the self-registration form", () => {
    renderComponent();
    expect(screen.getByText("Patient Self-Registration")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("10-digit mobile")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("First name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Last name")).toBeInTheDocument();
    expect(screen.getByText("Register")).toBeInTheDocument();
  });

  it("shows validation errors for empty form", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByText("Register"));

    await waitFor(() => {
      expect(screen.getByText("Invalid 10-digit mobile number")).toBeInTheDocument();
      expect(screen.getByText("First name required")).toBeInTheDocument();
      expect(screen.getByText("Last name required")).toBeInTheDocument();
    });
  });

  it("shows validation error for invalid mobile", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.type(screen.getByPlaceholderText("10-digit mobile"), "123");
    await user.type(screen.getByPlaceholderText("First name"), "John");
    await user.type(screen.getByPlaceholderText("Last name"), "Doe");
    await user.click(screen.getByText("Register"));

    await waitFor(() => {
      expect(screen.getByText("Invalid 10-digit mobile number")).toBeInTheDocument();
    });
  });

  it("submits valid form and shows success", async () => {
    const user = userEvent.setup();
    mockMutate.mockImplementation((_data: unknown, opts: { onSuccess: () => void }) => {
      opts.onSuccess();
    });
    renderComponent();

    await user.type(screen.getByPlaceholderText("10-digit mobile"), "9876543210");
    await user.type(screen.getByPlaceholderText("First name"), "John");
    await user.type(screen.getByPlaceholderText("Last name"), "Doe");
    await user.click(screen.getByText("Register"));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          mobileNo: "9876543210",
          firstName: "John",
          lastName: "Doe",
        }),
        expect.any(Object),
      );
    });

    await waitFor(() => {
      expect(screen.getByText("Registration Successful")).toBeInTheDocument();
    });
  });

  it("resets form after successful submission", async () => {
    const user = userEvent.setup();
    mockMutate.mockImplementation((_data: unknown, opts: { onSuccess: () => void }) => {
      opts.onSuccess();
    });
    renderComponent();

    await user.type(screen.getByPlaceholderText("10-digit mobile"), "9876543210");
    await user.type(screen.getByPlaceholderText("First name"), "John");
    await user.type(screen.getByPlaceholderText("Last name"), "Doe");
    await user.click(screen.getByText("Register"));

    await waitFor(() => {
      expect(screen.getByText("Registration Successful")).toBeInTheDocument();
    });

    await user.click(screen.getByText("New Registration"));

    await waitFor(() => {
      expect(screen.getByPlaceholderText("10-digit mobile")).toHaveValue("");
      expect(screen.getByPlaceholderText("First name")).toHaveValue("");
    });
  });
});
