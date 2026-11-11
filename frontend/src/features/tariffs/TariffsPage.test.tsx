import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { TariffsPage } from "./TariffsPage";

describe("TariffsPage", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders page header 'Tariffs & Concessions'", () => {
    render(<TariffsPage />);
    expect(screen.getByRole("heading", { name: /Tariffs & Concessions/i })).toBeInTheDocument();
  });

  it("shows clear 'Tariff management is not available yet' empty state", () => {
    render(<TariffsPage />);
    expect(screen.getByText("Tariff management is not available yet")).toBeInTheDocument();
    expect(
      screen.getByText(/Backend services for tariff and price master configuration are pending integration/i),
    ).toBeInTheDocument();
  });

  it("does not execute any network request to /api/v1/tariffs", () => {
    render(<TariffsPage />);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
