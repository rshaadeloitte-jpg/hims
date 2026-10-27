import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { Provider } from "../ui/provider";
import { Sidebar } from "./Sidebar";

function renderSidebar(path = "/billing/dashboard", collapsed = false, mobileOpen = false) {
  return render(
    <Provider>
      <MemoryRouter initialEntries={[path]}>
        <Sidebar collapsed={collapsed} mobileOpen={mobileOpen} onCloseMobile={() => {}} />
      </MemoryRouter>
    </Provider>,
  );
}

describe("Sidebar", () => {
  it("expands and marks the active parent section on /billing/dashboard", () => {
    renderSidebar();
    const overview = screen.getByRole("button", { name: /overview/i });
    expect(overview).toHaveAttribute("aria-expanded", "true");
    expect(overview).toHaveAttribute("data-active");
    expect(overview).toHaveAttribute("aria-current", "true");
  });

  it("exposes the active child route accessibly", () => {
    renderSidebar();
    const link = screen.getByRole("link", { name: "Billing Dashboard" });
    expect(link).toHaveAttribute("aria-current", "page");
    expect(link).toHaveAttribute("data-active");
  });

  it("renders the collapsed active section with a title", () => {
    renderSidebar("/billing/dashboard", true);
    expect(screen.getByTitle("Overview")).toBeInTheDocument();
    expect(screen.getByTitle("Overview")).toHaveAttribute("data-active");
  });

  it("renders navigation in the mobile drawer", () => {
    renderSidebar("/billing/dashboard", false, true);
    expect(screen.getByRole("link", { name: "Billing Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /overview/i })).toHaveAttribute("aria-expanded", "true");
  });
});
