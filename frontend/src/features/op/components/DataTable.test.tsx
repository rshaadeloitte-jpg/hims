import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { DataTable, type DataTableColumn } from "./DataTable";

interface Row { id: number; name: string; count: number }

const rows: Row[] = [
  { id: 1, name: "Alpha", count: 5 },
  { id: 2, name: "Bravo", count: 12 },
  { id: 3, name: "Charlie", count: 3 },
];

const columns: DataTableColumn<Row>[] = [
  { key: "id", header: "ID", accessor: (r) => r.id, width: "6ch" },
  { key: "name", header: "Name", accessor: (r) => r.name, sortable: true },
  { key: "count", header: "Count", accessor: (r) => r.count },
];

describe("DataTable", () => {
  it("renders one row per record and applies column accessors", () => {
    render(<DataTable rows={rows} columns={columns} />);
    expect(screen.getAllByRole("row")).toHaveLength(rows.length + 1); // +1 for header
    expect(screen.getByText("Alpha")).toBeInTheDocument();
    expect(screen.getByText("Bravo")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
  });

  it("renders an empty-state element when rows is empty", () => {
    render(<DataTable rows={[]} columns={columns} emptyState={<p>nope</p>} />);
    expect(screen.getByText("nope")).toBeInTheDocument();
  });

  it("renders skeleton placeholders while loading", () => {
    const { container } = render(<DataTable rows={[]} columns={columns} loading />);
    expect(container.querySelectorAll(".data-table-skeleton__row")).toHaveLength(5);
  });

  it("sorts by the active column when its header is clicked", () => {
    render(<DataTable rows={rows} columns={columns} />);
    const headerName = screen.getByRole("button", { name: /name/i });
    fireEvent.click(headerName);
    const tbodyRows = screen.getAllByRole("row").slice(1); // skip header
    expect(within(tbodyRows[0]).getByText("Alpha")).toBeInTheDocument();
    // Click again to flip to descending
    fireEvent.click(headerName);
    const tbodyRows2 = screen.getAllByRole("row").slice(1);
    expect(within(tbodyRows2[0]).getByText("Charlie")).toBeInTheDocument();
    // Third click clears sort
    fireEvent.click(headerName);
    const tbodyRows3 = screen.getAllByRole("row").slice(1);
    // Default order: as-supplied
    expect(within(tbodyRows3[0]).getByText("Alpha")).toBeInTheDocument();
  });

  it("invokes onRowClick when a row is clicked", () => {
    const onRowClick = vi.fn();
    render(<DataTable rows={rows} columns={columns} onRowClick={onRowClick} />);
    fireEvent.click(screen.getByText("Bravo"));
    expect(onRowClick).toHaveBeenCalledTimes(1);
    expect(onRowClick.mock.calls[0][0].id).toBe(2);
  });

  it("does not mark non-clickable rows with the row-clickable class", () => {
    const { container } = render(<DataTable rows={rows} columns={columns} />);
    expect(container.querySelectorAll("tr.row-clickable")).toHaveLength(0);
  });
});
