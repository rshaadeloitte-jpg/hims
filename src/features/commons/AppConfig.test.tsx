import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppConfigPage } from "./AppConfig";

/**
 * AppConfigPage smoke test — exercises the config list → form flow.
 * fetch is globally mocked to simulate the /commons/config endpoint.
 */

const mockFetch = vi.fn();
global.fetch = mockFetch;

function renderAppConfig() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AppConfigPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockFetch.mockReset();
});

afterEach(() => {
  mockFetch.mockReset();
});

describe("AppConfigPage", () => {
  it("renders the key-value config table with data from the API", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, configKey: "app.theme", configValue: "dark", description: "UI theme", unitId: null },
        { id: 2, configKey: "app.locale", configValue: "en-IN", description: "Language", unitId: 7 },
      ],
    });

    renderAppConfig();

    expect(screen.getByText("Application Configuration")).toBeInTheDocument();

    // Rows arrive from the mocked fetch
    await screen.findByText("app.theme");
    expect(screen.getByText("dark")).toBeInTheDocument();
    expect(screen.getByText("app.locale")).toBeInTheDocument();
    expect(screen.getByText("en-IN")).toBeInTheDocument();
    expect(screen.getAllByText("global").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("7")).toBeInTheDocument();
  });

  it("shows the empty state when no configs exist", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    renderAppConfig();

    await screen.findByText(/No configuration entries/i);
    expect(screen.getByText(/No configuration entries \(global scope\)/)).toBeInTheDocument();
  });

  it("opens the new-config form on + New Config click", async () => {
    const user = userEvent.setup();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    renderAppConfig();
    await screen.findByText(/No configuration entries/i);

    await user.click(screen.getByRole("button", { name: "+ New Config" }));

    expect(screen.getByLabelText(/Key/)).toBeInTheDocument();
    expect(screen.getByLabelText("Value")).toBeInTheDocument();
    expect(screen.getByLabelText("Description")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create" })).toBeInTheDocument();
  });

  it("PUTs the config and invalidates the list on save", async () => {
    const user = userEvent.setup();
    mockFetch
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => [],
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ id: 3, configKey: "app.beta", configValue: "true", description: "" }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => [
          { id: 3, configKey: "app.beta", configValue: "true", description: "", unitId: null },
        ],
      });

    renderAppConfig();
    await screen.findByText(/No configuration entries/i);

    await user.click(screen.getByRole("button", { name: "+ New Config" }));
    await user.type(screen.getByLabelText(/Key/), "app.beta");
    await user.type(screen.getByLabelText("Value"), "true");
    await user.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/commons/config"),
        expect.objectContaining({ method: "PUT" }),
      );
    });

    // After save, list is invalidated → refetch shows the new row
    await screen.findByText("app.beta");
  });
});
