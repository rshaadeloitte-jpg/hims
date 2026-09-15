import type { MasterRecord } from "../../../api/client";
import { listableFields, type EntityMeta } from "../entityMeta";
import { cellClass, formatCellValue } from "../format";

interface MasterTableProps {
  entity: string;
  meta: EntityMeta;
  rows: MasterRecord[];
  /** Called when a row is clicked (navigates to the edit screen). */
  onRowClick?: (record: MasterRecord) => void;
}

/**
 * Schema-driven data grid: columns are derived from the entity metadata
 * (fields flagged listable), cell rendering follows the field type
 * (boolean badge, number alignment, date formatting). No per-entity code.
 */
export function MasterTable({ entity, meta, rows, onRowClick }: MasterTableProps) {
  const columns = listableFields(meta);
  if (columns.length === 0) {
    return <p className="empty-state">No columns defined for {entity}.</p>;
  }
  return (
    <div className="table-wrap">
      <table className="data-table" aria-label={`${meta.label} records`}>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.name} scope="col">
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr
              key={String((row.id as number | undefined) ?? i)}
              onClick={() => onRowClick?.(row)}
              className={onRowClick ? "row-clickable" : undefined}
            >
              {columns.map((col) => (
                <td key={col.name} className={cellClass(col)}>
                  {col.type === "boolean" ? (
                    <span className={`badge ${row[col.name] ? "badge--success" : "badge--muted"}`}>
                      {formatCellValue(row[col.name], col)}
                    </span>
                  ) : (
                    formatCellValue(row[col.name], col)
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
