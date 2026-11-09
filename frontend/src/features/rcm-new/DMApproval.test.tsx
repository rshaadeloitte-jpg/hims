import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { DMApproval } from "./DMApproval";

function makeClaim(overrides: Record<string, unknown> = {}) {
  return {
    requestId: 1,
    kind: 0,
    patientId: 101,
    billId: 202,
    admissionId: 1,
    statusCode: 0,
    requestPayload: "{}",
    correlationId: "corr-123",
    ...overrides,
  };
}

function renderComp(claimId: number, opts: { onApproved?: () => void; onRejected?: () => void } = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <DMApproval claimId={claimId} onApproved={opts.onApproved} onRejected={opts.onRejected} />
    </QueryClientProvider>,
  );
}

describe("DMApproval", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("buttons enabled for a pending claim", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => makeClaim({ statusCode: 1 }),
    } as Response);

    renderComp(1);

    expect(screen.getByText("DM Approval — Claim #1")).toBeInTheDocument();
    // section aria-label kept
    expect(screen.getByLabelText("DM Approval Workflow")).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText("101")).toBeInTheDocument());

    // readable fields
    expect(screen.getByText("corr-123")).toBeInTheDocument();
    expect(screen.getByText("202")).toBeInTheDocument();
    expect(screen.getByText("SENT")).toBeInTheDocument();

    expect(screen.getByRole("button", { name: /^Approve$/i })).toBeEnabled();
    expect(screen.getByRole("button", { name: /^Reject$/i })).toBeEnabled();
  });

  it("buttons disabled for a final claim", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => makeClaim({ statusCode: 4 }),
    } as Response);

    renderComp(1);

    await waitFor(() => expect(screen.getByText("APPROVED")).toBeInTheDocument());

    expect(screen.getByRole("button", { name: /^Approve$/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /^Reject$/i })).toBeDisabled();
  });

  it("approve calls PATCH /api/v1/claims/1/approve", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce({
        ok: true,
        json: async () => makeClaim({ statusCode: 0 }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => makeClaim({ statusCode: 4 }),
      } as Response);

    const onApproved = vi.fn();
    renderComp(1, { onApproved });

    await waitFor(() => expect(screen.getByRole("button", { name: /^Approve$/i })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: /^Approve$/i }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining("/api/v1/claims/1/approve"),
        expect.objectContaining({ method: "PATCH" }),
      ),
    );
    await waitFor(() => expect(onApproved).toHaveBeenCalled());
  });

  it("shows loading and error states", async () => {
    // loading
    let resolveFetch!: (v: Response) => void;
    const pendingFetch = new Promise<Response>((res) => {
      resolveFetch = res;
    });
    vi.spyOn(globalThis, "fetch").mockReturnValue(pendingFetch as never);
    renderComp(1);
    expect(screen.getByText(/Loading claim/i)).toBeInTheDocument();
    resolveFetch({ ok: true, json: async () => makeClaim({ statusCode: 0 }) } as Response);
    await waitFor(() => expect(screen.getByText("INITIATED")).toBeInTheDocument());

    // error state for 404 handled via fresh render
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: false,
      status: 404,
      statusText: "Not Found",
      json: async () => ({ message: "Not found" }),
    } as unknown as Response);
    const qc2 = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={qc2}>
        <DMApproval claimId={2} />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(screen.getByText(/Not found/i)).toBeInTheDocument());
  });

  it("shows backend error on 409", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce({
        ok: true,
        json: async () => makeClaim({ statusCode: 1 }),
      } as Response)
      .mockResolvedValueOnce({
        ok: false,
        status: 409,
        statusText: "Conflict",
        json: async () => ({ message: "Claim already final" }),
      } as unknown as Response);

    renderComp(1);
    await waitFor(() => expect(screen.getByRole("button", { name: /^Approve$/i })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: /^Approve$/i }));
    await waitFor(() => expect(screen.getByText(/Claim already final/i)).toBeInTheDocument());
  });
});
