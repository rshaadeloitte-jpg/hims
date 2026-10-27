import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { Provider } from "./components/ui/provider";
import { Sidebar } from "./components/layout/Sidebar";

describe("inspect", () => {
  it("prints expanded", () => {
    render(
      <Provider>
        <MemoryRouter initialEntries={["/billing/dashboard"]}>
          <Sidebar collapsed={false} mobileOpen={false} onCloseMobile={() => {}} />
        </MemoryRouter>
      </Provider>,
    );
    const btn = screen.getByRole("button", { name: /overview/i });
    const cs = window.getComputedStyle(btn);
    console.log("EXP_BG=" + cs.backgroundColor);
    console.log("EXP_COLOR=" + cs.color);
    const p = btn.querySelector("p") as HTMLElement;
    console.log("P_COLOR=" + window.getComputedStyle(p).color);
    expect(true).toBe(true);
  });
});
