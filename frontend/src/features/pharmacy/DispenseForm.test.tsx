import { describe, test, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DispenseForm } from "./DispenseForm";

const prescription = {
  prescriptionId: 1,
  patientId: 101,
  prescriberId: 7,
  statusCode: 1,
  details: [
    {
      lineNo: 1,
      itemId: 5001,
      qty: 5,
      dose: "1 tablet",
      frequency: "twice daily",
    },
  ],
};

function saleDetail(overrides: any = {}) {
  return {
    saleId: 1,
    lineNo: 1,
    itemId: 5001,
    batchId: null,
    qty: 5,
    pendingQty: 5,
    dispensed: false,
    verified: false,
    partialDispense: false,
    ...overrides,
  };
}

function makeSale(overrides: any = {}) {
  return {
    saleId: 1,
    patientId: 101,
    prescriptionId: 1,
    statusCode: 1,
    dispensed: false,
    details: [saleDetail()],
    ...overrides,
  };
}

const fetchMock = vi.fn();

function renderForm(presc: any = prescription) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <DispenseForm prescription={presc} />
    </QueryClientProvider>,
  );
}

function setupFetch(handler: (url: string, init?: RequestInit) => any) {
  fetchMock.mockImplementation(async (url: RequestInfo | URL, init?: RequestInit) => {
    const res = await handler(String(url), init);
    return {
      ok: res.status >= 200 && res.status < 300,
      status: res.status,
      json: async () => res.body,
      text: async () => JSON.stringify(res.body),
    };
  });
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("DispenseForm", () => {
  test("full dispense creates a sale then dispenses", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      if (url === "/api/v1/item-sales" && method === "POST") {
        return { status: 201, body: makeSale() };
      }
      if (url === "/api/v1/item-sales/1/dispense" && method === "POST") {
        return { status: 200, body: makeSale({ statusCode: 3, dispensed: true, details: [saleDetail({ pendingQty: 0, dispensed: true })] }) };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByTestId("confirm-dispense");
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Dispensed successfully \(sale #1\)/);

    const creates = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales" && ((i as any)?.method ?? "GET") === "POST");
    const dispenses = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales/1/dispense" && ((i as any)?.method ?? "GET") === "POST");
    expect(creates.length).toBe(1);
    expect(dispenses.length).toBe(1);
  });

  test("partial dispense sends the chosen qty", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      if (url === "/api/v1/item-sales" && method === "POST") {
        return { status: 201, body: makeSale() };
      }
      if (url === "/api/v1/item-sales/1/dispense" && method === "POST") {
        return { status: 200, body: makeSale({ statusCode: 2, dispensed: false, details: [saleDetail({ pendingQty: 2, dispensed: false, partialDispense: true })] }) };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByTestId("qty-1");
    fireEvent.change(screen.getByTestId("qty-1"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Dispensed successfully \(sale #1\)/);

    const dispenseCall = fetchMock.mock.calls.find(([u, i]) => String(u) === "/api/v1/item-sales/1/dispense" && ((i as any)?.method ?? "GET") === "POST");
    expect(dispenseCall).toBeTruthy();
    const body = JSON.parse((dispenseCall![1] as any).body);
    expect(body.lines[0].qty).toBe(3);
  });

  test("existing sale resumes with its pendingQty and sends no POST /api/v1/item-sales", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [makeSale({ saleId: 7, details: [saleDetail({ saleId: 7, pendingQty: 4 })] })] };
      }
      if (url === "/api/v1/item-sales/7/dispense" && method === "POST") {
        return { status: 200, body: makeSale({ saleId: 7, statusCode: 3, dispensed: true }) };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByText(/pending: 4/);
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Dispensed successfully \(sale #7\)/);

    const creates = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales" && ((i as any)?.method ?? "GET") === "POST");
    expect(creates.length).toBe(0);
  });

  test("fully dispensed sale disables confirm", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [makeSale({ statusCode: 3, dispensed: true, details: [saleDetail({ pendingQty: 0, dispensed: true })] })] };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByText("Fully dispensed");
    expect(screen.getByTestId("confirm-dispense")).toBeDisabled();
  });

  test("qty over pending blocks confirm", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByTestId("qty-1");
    fireEvent.change(screen.getByTestId("qty-1"), { target: { value: "99" } });
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Quantity must be <= pending/);

    const creates = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales" && ((i as any)?.method ?? "GET") === "POST");
    const dispenses = fetchMock.mock.calls.filter(([u]) => String(u).includes("/dispense"));
    expect(creates.length).toBe(0);
    expect(dispenses.length).toBe(0);
  });

  test("dispense failure then retry reuses the same sale id with no second create", async () => {
    let dispenseCount = 0;
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      if (url === "/api/v1/item-sales" && method === "POST") {
        return { status: 201, body: makeSale() };
      }
      if (url === "/api/v1/item-sales/1/dispense" && method === "POST") {
        dispenseCount++;
        if (dispenseCount === 1) {
          return { status: 400, body: { errorCode: "BEXA003", message: "Insufficient stock" } };
        }
        return { status: 200, body: makeSale({ statusCode: 3, dispensed: true }) };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByTestId("confirm-dispense");
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Retry Dispense/);
    fireEvent.click(screen.getByTestId("retry-dispense"));

    await screen.findByText(/Dispensed successfully \(sale #1\)/);

    const creates = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales" && ((i as any)?.method ?? "GET") === "POST");
    expect(creates.length).toBe(1);
  });

  test("double click sends exactly one create and one dispense", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      if (url === "/api/v1/item-sales" && method === "POST") {
        return { status: 201, body: makeSale() };
      }
      if (url === "/api/v1/item-sales/1/dispense" && method === "POST") {
        return { status: 200, body: makeSale({ statusCode: 3, dispensed: true }) };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByTestId("confirm-dispense");
    const btn = screen.getByTestId("confirm-dispense");
    fireEvent.click(btn);
    fireEvent.click(btn);

    await screen.findByText(/Dispensed successfully \(sale #1\)/);

    const creates = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales" && ((i as any)?.method ?? "GET") === "POST");
    const dispenses = fetchMock.mock.calls.filter(([u, i]) => String(u) === "/api/v1/item-sales/1/dispense" && ((i as any)?.method ?? "GET") === "POST");
    expect(creates.length).toBe(1);
    expect(dispenses.length).toBe(1);
  });

  test("server stock error text is shown and no success message appears", async () => {
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      if (url === "/api/v1/item-sales" && method === "POST") {
        return { status: 201, body: makeSale() };
      }
      if (url === "/api/v1/item-sales/1/dispense" && method === "POST") {
        return { status: 400, body: { errorCode: "BEXA003", message: "Insufficient stock" } };
      }
      return { status: 200, body: [] };
    });

    renderForm();
    await screen.findByTestId("confirm-dispense");
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Insufficient stock/);
    expect(screen.queryByText(/Dispensed successfully/)).toBeNull();
  });

  test("fractional qty line is blocked", async () => {
    const fracPrescription = {
      ...prescription,
      details: [{ lineNo: 1, itemId: 5001, qty: 5.5, dose: "1 tablet", frequency: "bid" }],
    };
    setupFetch((url, init) => {
      const method = init?.method ?? "GET";
      if (url.includes("/api/v1/item-sales") && url.includes("prescriptionId=") && method === "GET") {
        return { status: 200, body: [] };
      }
      return { status: 200, body: [] };
    });

    renderForm(fracPrescription);
    await screen.findByTestId("confirm-dispense");
    fireEvent.click(screen.getByTestId("confirm-dispense"));

    await screen.findByText(/Fractional quantities are not supported/);
  });
});
