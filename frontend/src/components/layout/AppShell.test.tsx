import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { Provider } from "../ui/provider";
import { AppShell } from "./AppShell";

function renderShell(path = "/masters") {
  return render(
    <Provider>
      <MemoryRouter initialEntries={[path]}>
        <AppShell>
          <div>page-body</div>
        </AppShell>
      </MemoryRouter>
    </Provider>,
  );
}

describe("AppShell", () => {
  it("renders Deloitte brand, sidebar sections and page content", () => {
    renderShell();
    expect(screen.getByLabelText("Deloitte HIMS home")).toBeInTheDocument();
    expect(screen.getByText("page-body")).toBeInTheDocument();
    // Sidebar groups
    expect(screen.getByText("Front Office")).toBeInTheDocument();
    expect(screen.getByText("Billing & Finance")).toBeInTheDocument();
    // Global search + user menu trigger
    expect(screen.getByLabelText("Global search")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /demo user/i })).toBeInTheDocument();
  });

  it("marks the active nav item and opens the user dropdown", async () => {
    const user = userEvent.setup();
    renderShell("/billing");
    expect(screen.getByRole("link", { name: "Bills" })).toHaveAttribute(
      "aria-current",
      "page",
    );

    await user.click(screen.getByRole("button", { name: /demo user/i }));
    expect(screen.getByRole("menu", { name: /user profile/i })).toBeInTheDocument();
    expect(screen.getByRole("menuitem", { name: /sign out/i })).toBeInTheDocument();
  });

  it("collapses the sidebar via the header toggle", async () => {
    const user = userEvent.setup();
    renderShell();
    expect(screen.getByTestId("app-shell")).not.toHaveAttribute("data-collapsed");
    await user.click(screen.getByLabelText("Collapse sidebar"));
    expect(screen.getByTestId("app-shell")).toHaveAttribute("data-collapsed");
  });
});
