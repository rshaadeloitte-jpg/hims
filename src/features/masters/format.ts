import type { FieldMeta } from "./entityMeta";

/** Renders a raw record value for a grid cell / read-only display. */
export function formatCellValue(value: unknown, field: FieldMeta): string {
  if (value === null || value === undefined || value === "") {
    return "—";
  }
  switch (field.type) {
    case "boolean":
      return value ? "Active" : "Inactive";
    case "number": {
      const n = typeof value === "number" ? value : Number(value);
      return Number.isNaN(n) ? String(value) : n.toLocaleString();
    }
    case "date": {
      const d = new Date(String(value));
      return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString();
    }
    default:
      return String(value);
  }
}

/** Grid cell class for type-based alignment (numbers right-aligned). */
export function cellClass(field: FieldMeta): string {
  return field.type === "number" ? "cell cell--num" : "cell";
}
