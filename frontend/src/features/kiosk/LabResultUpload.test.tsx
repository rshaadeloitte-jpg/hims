import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LabResultUpload } from "./LabResultUpload";

vi.mock("./queries", () => ({
  useUploadLabResult: vi.fn(),
}));

const { useUploadLabResult } = await import("./queries");
const mockMutate = vi.fn();

function renderComponent() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <LabResultUpload />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useUploadLabResult as ReturnType<typeof vi.fn>).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
  });
});

describe("LabResultUpload", () => {
  it("renders the lab result upload form", () => {
    renderComponent();
    expect(screen.getByText("Lab Result Upload")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Token number")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Visit ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("e.g. CBC, Glucose")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("e.g. 135")).toBeInTheDocument();
    expect(screen.getByText("Upload Result")).toBeInTheDocument();
  });

  it("shows validation errors for empty form", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByText("Upload Result"));

    await waitFor(() => {
      expect(screen.getByText("Token number required")).toBeInTheDocument();
      expect(screen.getByText("Visit ID required")).toBeInTheDocument();
      expect(screen.getByText("Test name required")).toBeInTheDocument();
      expect(screen.getByText("Result value required")).toBeInTheDocument();
    });
  });

  it("shows file required error when no file attached", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.type(screen.getByPlaceholderText("Token number"), "TKN-001");
    await user.type(screen.getByPlaceholderText("Visit ID"), "VIS-001");
    await user.type(screen.getByPlaceholderText("e.g. CBC, Glucose"), "CBC");
    await user.type(screen.getByPlaceholderText("e.g. 135"), "135");

    await user.click(screen.getByText("Upload Result"));

    await waitFor(() => {
      expect(screen.getByText("File required")).toBeInTheDocument();
    });
  });

  it("submits valid form and shows success", async () => {
    const user = userEvent.setup();
    mockMutate.mockImplementation((_data: unknown, opts: { onSuccess: () => void }) => {
      opts.onSuccess();
    });
    renderComponent();

    await user.type(screen.getByPlaceholderText("Token number"), "TKN-001");
    await user.type(screen.getByPlaceholderText("Visit ID"), "VIS-001");
    await user.type(screen.getByPlaceholderText("e.g. CBC, Glucose"), "CBC");
    await user.type(screen.getByPlaceholderText("e.g. 135"), "135");

    const dateInput = screen.getByText("Result Date *").closest(".field")?.querySelector("input[type=date]") as HTMLInputElement;
    await user.type(dateInput, "2026-09-15");

    const file = new File(["test content"], "result.pdf", { type: "application/pdf" });
    const fileInput = screen.getByText("Attach File *").closest(".field")?.querySelector("input[type=file]") as HTMLInputElement;
    await user.upload(fileInput, file);

    await user.click(screen.getByText("Upload Result"));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          tokenNo: "TKN-001",
          visitId: "VIS-001",
          testName: "CBC",
          resultValue: "135",
        }),
        expect.any(Object),
      );
    });

    await waitFor(() => {
      expect(screen.getByText("Upload Successful")).toBeInTheDocument();
    });
  });

  it("resets form after successful upload", async () => {
    const user = userEvent.setup();
    mockMutate.mockImplementation((_data: unknown, opts: { onSuccess: () => void }) => {
      opts.onSuccess();
    });
    renderComponent();

    await user.type(screen.getByPlaceholderText("Token number"), "TKN-001");
    await user.type(screen.getByPlaceholderText("Visit ID"), "VIS-001");
    await user.type(screen.getByPlaceholderText("e.g. CBC, Glucose"), "CBC");
    await user.type(screen.getByPlaceholderText("e.g. 135"), "135");
    const dateInput = screen.getByText("Result Date *").closest(".field")?.querySelector("input[type=date]") as HTMLInputElement;
    await user.type(dateInput, "2026-09-15");
    const file = new File(["test"], "r.pdf", { type: "application/pdf" });
    const fileInput = screen.getByText("Attach File *").closest(".field")?.querySelector("input[type=file]") as HTMLInputElement;
    await user.upload(fileInput, file);
    await user.click(screen.getByText("Upload Result"));

    await waitFor(() => {
      expect(screen.getByText("Upload Successful")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Upload Another"));

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Token number")).toHaveValue("");
      expect(screen.getByPlaceholderText("Visit ID")).toHaveValue("");
    });
  });
});
