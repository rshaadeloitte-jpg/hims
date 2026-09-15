import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { FeedbackForm } from "./FeedbackForm";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const mockSubmit = vi.fn();
const mockCancel = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
});

describe("FeedbackForm", () => {
  it("renders 5 star rating questions", () => {
    render(
      <FeedbackForm grievanceId={42} submitting={false} onSubmit={mockSubmit} onCancel={mockCancel} />,
      { wrapper: wrapper() },
    );

    expect(screen.getByText("Satisfaction Feedback")).toBeTruthy();
    expect(screen.getByText(/How satisfied are you with the resolution/)).toBeTruthy();
    expect(screen.getByText(/How would you rate the response time/)).toBeTruthy();
    expect(screen.getByText(/How clear was the communication/)).toBeTruthy();
    expect(screen.getByText(/How professional was the support staff/)).toBeTruthy();
    expect(screen.getByText(/Overall, how likely are you/)).toBeTruthy();
  });

  it("renders 5 star buttons per question", () => {
    render(
      <FeedbackForm grievanceId={42} submitting={false} onSubmit={mockSubmit} onCancel={mockCancel} />,
      { wrapper: wrapper() },
    );

    // 5 questions x 5 stars each = 25 star buttons
    const starButtons = screen.getAllByRole("button", { name: /\d star/ });
    expect(starButtons).toHaveLength(25);
  });

  it("renders free-text and email inputs", () => {
    render(
      <FeedbackForm grievanceId={42} submitting={false} onSubmit={mockSubmit} onCancel={mockCancel} />,
      { wrapper: wrapper() },
    );

    expect(screen.getByLabelText(/Additional Comments/)).toBeTruthy();
    expect(screen.getByLabelText(/Email/)).toBeTruthy();
  });

  it("calls onCancel when Cancel is clicked", () => {
    render(
      <FeedbackForm grievanceId={42} submitting={false} onSubmit={mockSubmit} onCancel={mockCancel} />,
      { wrapper: wrapper() },
    );

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(mockCancel).toHaveBeenCalledOnce();
  });

  it("disables submit when not all questions rated", async () => {
    render(
      <FeedbackForm grievanceId={42} submitting={false} onSubmit={mockSubmit} onCancel={mockCancel} />,
      { wrapper: wrapper() },
    );

    const submitBtn = screen.getByRole("button", { name: /submit feedback/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      // Should show validation errors, not call submit
      expect(mockSubmit).not.toHaveBeenCalled();
    });
  });

  it("submits when all questions are rated", async () => {
    render(
      <FeedbackForm grievanceId={42} submitting={false} onSubmit={mockSubmit} onCancel={mockCancel} />,
      { wrapper: wrapper() },
    );

    // Click 5 stars for each question
    const stars = screen.getAllByRole("button", { name: "5 stars" });
    expect(stars.length).toBeGreaterThanOrEqual(5);
    stars.slice(0, 5).forEach((s) => fireEvent.click(s));

    const submitBtn = screen.getByRole("button", { name: /submit feedback/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalledOnce();
      const payload = mockSubmit.mock.calls[0][0];
      expect(payload.grievanceId).toBe(42);
      expect(payload.question1).toBe(5);
      expect(payload.question2).toBe(5);
      expect(payload.question3).toBe(5);
      expect(payload.question4).toBe(5);
      expect(payload.question5).toBe(5);
    });
  });
});
