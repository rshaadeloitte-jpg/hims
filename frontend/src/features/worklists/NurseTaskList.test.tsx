import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { NurseTaskList } from "./NurseTaskList";
import { useWorklistPatients } from "./queries";

vi.mock("./queries");

const mockedUseWorklistPatients = vi.mocked(useWorklistPatients);

function renderNurseTaskList() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <NurseTaskList />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("NurseTaskList", () => {
  it("renders worklist summary from backend map", () => {
    mockedUseWorklistPatients.mockReturnValue({
      data: { admissionId: 100, tabs: [{ name: "tasks", open: 2, total: 5 }], openTasks: 2, totalTasks: 5 },
      isLoading: false,
    } as any);

    renderNurseTaskList();

    const input = screen.getByRole("spinbutton");
    fireEvent.change(input, { target: { value: "100" } });

    expect(screen.getByText(/Admission 100/)).toBeInTheDocument();
    expect(screen.getByText(/2 open \/ 5 total/)).toBeInTheDocument();
    expect(screen.getByText(/tasks: 2\/5/)).toBeInTheDocument();
  });
});
