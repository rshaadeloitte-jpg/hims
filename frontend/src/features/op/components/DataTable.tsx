import { type ReactNode, useMemo, useState } from "react";
import { Button, Table } from "@chakra-ui/react";

/**
 * DataTable — schema-driven grid. Reusable across the OP module (and
 * future modules) for any list view. Each {@link DataTableColumn} describes
 * one column: where to read the value, how to render it, and how to sort.
 *
 * Why not just rely on a CSS grid? Two reasons:
 *   1. Sorting (per-column, three-state: asc -> desc -> unsorted) is a screen
 *      behavior, not a CSS one. Centralising the logic keeps screen code
 *      declarative (the columns array IS the contract).
 *   2. Empty-state + loading-state handling is consistent across OP pages
 *      (AppointmentListPage is the largest consumer). A small component
 *      prevents 5 different screens rolling their own.
 *
 * No external table library — keeps the bundle small and gives the legacy
 * app the same visual fingerprint (sans serif, soft borders, no zebra).
 */
export type SortDir = "asc" | "desc";

export interface DataTableColumn<Row> {
  /** Unique key for the column — also used as the sort key. */
  key: string;
  /** Display header. */
  header: string;
  /** Cell value extractor. */
  accessor: (row: Row) => unknown;
  /** Optional cell renderer (overrides the default `String(value)`). */
  render?: (row: Row) => ReactNode;
  /** Whether the column is sortable. Default true. */
  sortable?: boolean;
  /** Tailwind / token class for the cell — e.g. "cell--right" for numbers. */
  className?: string;
  /** Optional fixed width (e.g. "8ch" for the ID column). */
  width?: string;
}

interface DataTableProps<Row> {
  rows: readonly Row[];
  columns: ReadonlyArray<DataTableColumn<Row>>;
  /** Stable row key — defaults to row.id. */
  rowKey?: (row: Row) => string | number;
  /** Click handler — turns rows into buttons. */
  onRowClick?: (row: Row) => void;
  /** Whether the table is currently loading (shows skeleton rows). */
  loading?: boolean;
  /** Empty-state element shown when rows is empty AND not loading. */
  emptyState?: ReactNode;
  /** ARIA label for the table element. */
  ariaLabel?: string;
}

export function DataTable<Row>({
  rows,
  columns,
  rowKey,
  onRowClick,
  loading,
  emptyState,
  ariaLabel,
}: DataTableProps<Row>) {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>("asc");

  const sorted = useMemo(() => {
    if (!sortKey) return rows;
    const col = columns.find((c) => c.key === sortKey);
    if (!col) return rows;
    const acc = col.accessor;
    const out = [...rows];
    out.sort((a, b) => {
      const av = acc(a);
      const bv = acc(b);
      if (av == null && bv == null) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;
      if (av < bv) return sortDir === "asc" ? -1 : 1;
      if (av > bv) return sortDir === "asc" ? 1 : -1;
      return 0;
    });
    return out;
  }, [rows, columns, sortKey, sortDir]);

  const toggleSort = (key: string) => {
    if (sortKey !== key) {
      setSortKey(key);
      setSortDir("asc");
    } else if (sortDir === "asc") {
      setSortDir("desc");
    } else {
      setSortKey(null);
      setSortDir("asc");
    }
  };

  if (loading) {
    return (
      <div className="data-table-skeleton" role="status" aria-busy="true">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="data-table-skeleton__row" />
        ))}
      </div>
    );
  }

  if (rows.length === 0) {
    return <div className="data-table-empty">{emptyState ?? "No records."}</div>;
  }

  const keyFn = rowKey ?? ((r: Row) => (r as { id?: unknown }).id as string | number);

  return (
    <div className="data-table-wrap">
      <Table.Root size="sm" variant="line" aria-label={ariaLabel}>
        <Table.Header>
          <Table.Row>
            {columns.map((col) => {
              const sortable = col.sortable !== false;
              const active = sortKey === col.key;
              return (
                <Table.ColumnHeader
                  key={col.key}
                  fontSize="xs"
                  color="gray.600"
                  textTransform="uppercase"
                  scope="col"
                  aria-sort={active ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                  style={col.width ? { width: col.width } : undefined}
                >
                  {sortable ? (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => toggleSort(col.key)}
                    >
                      {col.header}
                      <span aria-hidden="true" className="data-table__sort-indicator">
                        {active ? (sortDir === "asc" ? "▲" : "▼") : "↕"}
                      </span>
                    </Button>
                  ) : (
                    col.header
                  )}
                </Table.ColumnHeader>
              );
            })}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {sorted.map((row) => {
            const clickable = typeof onRowClick === "function";
            return (
              <Table.Row
                key={keyFn(row)}
                onClick={clickable ? () => onRowClick?.(row) : undefined}
                className={clickable ? "row-clickable" : undefined}
                tabIndex={clickable ? 0 : undefined}
                onKeyDown={
                  clickable
                    ? (e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          onRowClick?.(row);
                        }
                      }
                    : undefined
                }
              >
                {columns.map((col) => (
                  <Table.Cell key={col.key} className={col.className}>
                    {col.render ? col.render(row) : defaultRender(col.accessor(row))}
                  </Table.Cell>
                ))}
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table.Root>
    </div>
  );
}

function defaultRender(value: unknown): ReactNode {
  if (value === null || value === undefined) return <span className="muted">—</span>;
  if (typeof value === "boolean") {
    return <span className={`badge ${value ? "badge--success" : "badge--muted"}`}>{value ? "Yes" : "No"}</span>;
  }
  if (value instanceof Date) return value.toLocaleString();
  return String(value);
}
