import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { ConfirmDialog } from "./ConfirmDialog";

describe("ConfirmDialog", () => {
  it("renders title and body when open", () => {
    render(
      <ConfirmDialog
        open
        title="Cancel?"
        body={<p>Are you sure?</p>}
        onCancel={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );
    expect(screen.getByText("Cancel?")).toBeInTheDocument();
    expect(screen.getByText("Are you sure?")).toBeInTheDocument();
  });

  it("invokes onCancel when the cancel button is clicked", () => {
    const onCancel = vi.fn();
    render(
      <ConfirmDialog open title="X" body="X" onCancel={onCancel} onConfirm={vi.fn()} />,
    );
    fireEvent.click(screen.getByRole("button", { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("invokes onConfirm when the confirm button is clicked", () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog open title="X" body="X" onCancel={vi.fn()} onConfirm={onConfirm} />,
    );
    fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("blocks onConfirm when the reason is required and blank", () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        open
        title="Cancel"
        body={<p>Body</p>}
        onCancel={vi.fn()}
        onConfirm={onConfirm}
        reasonField={{ label: "Reason", required: true, value: "", onChange: vi.fn() }}
      />,
    );
    const confirmBtn = screen.getByRole("button", { name: /confirm/i });
    expect(confirmBtn).toBeDisabled();
    fireEvent.click(confirmBtn);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("uses the danger tone for destructive confirms", () => {
    render(
      <ConfirmDialog
        open
        title="Delete"
        body="X"
        confirmTone="danger"
        onCancel={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: /confirm/i }).className).toContain("btn-danger");
  });

  it("renders nothing when closed (no DOM contribution from the closed <dialog>)", () => {
    const { container } = render(
      <ConfirmDialog open={false} title="X" body="X" onCancel={vi.fn()} onConfirm={vi.fn()} />,
    );
    expect(container.querySelector("dialog")).not.toBeVisible();
  });
});
