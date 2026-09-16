import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GoalsInterventions } from "./GoalsInterventions";
import { useCarePlansByAdmission, useUpdateGoalStatus, useUpdateInterventionStatus } from "./queries";

vi.mock("./queries");

const mockedUseCarePlansByAdmission = vi.mocked(useCarePlansByAdmission);
const mockedUseUpdateGoalStatus = vi.mocked(useUpdateGoalStatus);
const mockedUseUpdateInterventionStatus = vi.mocked(useUpdateInterventionStatus);

function renderGoalsInterventions(admissionId: number) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <GoalsInterventions admissionId={admissionId} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("GoalsInterventions", () => {
  it("renders loading state", () => {
    mockedUseCarePlansByAdmission.mockReturnValue({ data: undefined, isLoading: true } as any);
    mockedUseUpdateGoalStatus.mockReturnValue({ mutate: vi.fn() } as any);
    mockedUseUpdateInterventionStatus.mockReturnValue({ mutate: vi.fn() } as any);

    renderGoalsInterventions(100);

    expect(screen.getByText("Loading care plans…")).toBeInTheDocument();
  });

  it("renders error state", () => {
    mockedUseCarePlansByAdmission.mockReturnValue({ data: undefined, isLoading: false, isError: true } as any);
    mockedUseUpdateGoalStatus.mockReturnValue({ mutate: vi.fn() } as any);
    mockedUseUpdateInterventionStatus.mockReturnValue({ mutate: vi.fn() } as any);

    renderGoalsInterventions(100);

    expect(screen.getByText("Failed to load care plans.")).toBeInTheDocument();
  });

  it("renders empty state", () => {
    mockedUseCarePlansByAdmission.mockReturnValue({ data: [], isLoading: false, isError: false } as any);
    mockedUseUpdateGoalStatus.mockReturnValue({ mutate: vi.fn() } as any);
    mockedUseUpdateInterventionStatus.mockReturnValue({ mutate: vi.fn() } as any);

    renderGoalsInterventions(100);

    expect(screen.getByText(/No care plans found/)).toBeInTheDocument();
  });

  it("renders care plans with goals and interventions", () => {
    mockedUseCarePlansByAdmission.mockReturnValue({
      data: [
        {
          carePlanId: 1,
          admissionId: 100,
          patientId: 50,
          title: "Post-op care",
          description: "Monitor vitals",
          goals: [
            { goalId: 1, carePlanId: 1, goalText: "Pain under control", status: "ACTIVE", createdAt: "2026-01-01" },
          ],
          interventions: [
            { interventionId: 1, carePlanId: 1, interventionText: "Check vitals q4h", status: "SCHEDULED", createdAt: "2026-01-01" },
          ],
          resolved: false,
          createdAt: "2026-01-01",
        },
      ],
      isLoading: false,
      isError: false,
    } as any);
    mockedUseUpdateGoalStatus.mockReturnValue({ mutate: vi.fn() } as any);
    mockedUseUpdateInterventionStatus.mockReturnValue({ mutate: vi.fn() } as any);

    renderGoalsInterventions(100);

    expect(screen.getByText("Post-op care")).toBeInTheDocument();
    expect(screen.getByText("Monitor vitals")).toBeInTheDocument();
    expect(screen.getByText("Pain under control")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /Interventions/i }));
    expect(screen.getByText("Check vitals q4h")).toBeInTheDocument();
  });

  it("renders badge counts", () => {
    mockedUseCarePlansByAdmission.mockReturnValue({
      data: [
        {
          carePlanId: 1,
          admissionId: 100,
          patientId: 50,
          title: "Test Plan",
          goals: [
            { goalId: 1, carePlanId: 1, goalText: "Goal 1", status: "ACTIVE", createdAt: "2026-01-01" },
            { goalId: 2, carePlanId: 1, goalText: "Goal 2", status: "ACHIEVED", createdAt: "2026-01-01" },
          ],
          interventions: [
            { interventionId: 1, carePlanId: 1, interventionText: "Int 1", status: "COMPLETED", createdAt: "2026-01-01" },
            { interventionId: 2, carePlanId: 1, interventionText: "Int 2", status: "SCHEDULED", createdAt: "2026-01-01" },
          ],
          resolved: false,
          createdAt: "2026-01-01",
        },
      ],
      isLoading: false,
      isError: false,
    } as any);
    mockedUseUpdateGoalStatus.mockReturnValue({ mutate: vi.fn() } as any);
    mockedUseUpdateInterventionStatus.mockReturnValue({ mutate: vi.fn() } as any);

    renderGoalsInterventions(100);

    // Active goals and active interventions badges both show 1
    expect(screen.getAllByText("1").length).toBeGreaterThanOrEqual(2);
  });

  it("renders resolved badge for resolved care plan", () => {
    mockedUseCarePlansByAdmission.mockReturnValue({
      data: [
        {
          carePlanId: 1,
          admissionId: 100,
          patientId: 50,
          title: "Resolved Plan",
          goals: [],
          interventions: [],
          resolved: true,
          createdAt: "2026-01-01",
        },
      ],
      isLoading: false,
      isError: false,
    } as any);
    mockedUseUpdateGoalStatus.mockReturnValue({ mutate: vi.fn() } as any);
    mockedUseUpdateInterventionStatus.mockReturnValue({ mutate: vi.fn() } as any);

    renderGoalsInterventions(100);

    expect(screen.getByText("Resolved")).toBeInTheDocument();
  });
});
