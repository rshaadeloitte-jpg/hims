import { useState, useMemo } from "react";
import { Table } from "@chakra-ui/react";
import { HimsSelect } from "../../components/ui";
import {
  useItemEnquiry,
  useStockLevelTrend,
} from "./queries";

/** Vercel-style area chart for stock levels */
function StockTrendChart({ data, color = "#6366f1" }: { data: { date: string; closingStock: number }[]; color?: string }) {
  const max = Math.max(...data.map((d) => d.closingStock), 1);
  const width = data.length * 12;
  const height = 100;

  const points = data.map((d, i) => {
    const x = i * 12 + 6;
    const y = height - (d.closingStock / max) * (height - 10) - 5;
    return { x, y, date: d.date, value: d.closingStock };
  });

  const pathD = points.map((p, i) => `${i === 0 ? "M" : "L"} ${p.x} ${p.y}`).join(" ");
  const areaD = `${pathD} L ${points[points.length - 1].x} ${height} L ${points[0].x} ${height} Z`;

  return (
    <div className="mini-chart">
      <svg viewBox={`0 0 ${width} ${height}`} className="mini-chart__svg" preserveAspectRatio="none">
        <defs>
          <linearGradient id={`grad-${color.replace("#", "")}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.3" />
            <stop offset="100%" stopColor={color} stopOpacity="0.05" />
          </linearGradient>
        </defs>
        <path d={areaD} fill={`url(#grad-${color.replace("#", "")})`} />
        {points.map((p, i) => (
          <circle key={i} cx={p.x} cy={p.y} r={2} fill={color}>
            <title>{p.date}: {p.value} units</title>
          </circle>
        ))}
        <path d={pathD} fill="none" stroke={color} strokeWidth="2" />
      </svg>
      <div className="mini-chart__labels">
        {data.map((d, i) => (
          <span key={i} className="mini-chart__label">{d.date.slice(5)}</span>
        ))}
      </div>
    </div>
  );
}

/** Simple horizontal bar for stock level visualization */
function StockBar({ label, current, max, color }: { label: string; current: number; max: number; color: string }) {
  const pct = max > 0 ? Math.min((current / max) * 100, 100) : 0;
  return (
    <div className="stock-bar">
      <div className="stock-bar__header">
        <span className="stock-bar__label">{label}</span>
        <span className="stock-bar__value">{current} / {max}</span>
      </div>
      <div className="stock-bar__track">
        <div className="stock-bar__fill" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
    </div>
  );
}

export function StockDashboard() {
  const [selectedItemId, setSelectedItemId] = useState<number>(0);
  const { data: items = [], isLoading } = useItemEnquiry({});
  const { data: trend = [] } = useStockLevelTrend(selectedItemId, 14);

  const reorderAlerts = useMemo(
    () => items.filter((item) => item.currentStock <= item.reorderLevel),
    [items],
  );

  const totalItems = items.length;
  const totalStockValue = items.reduce((sum, i) => sum + i.currentStock, 0);
  const lowStockCount = reorderAlerts.length;
  const healthyStockCount = totalItems - lowStockCount;

  if (isLoading) {
    return (
      <div className="stock-dashboard">
        <div className="stock-dashboard__skeleton">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="skeleton skeleton--card" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="stock-dashboard">
      {/* Summary cards */}
      <div className="stock-dashboard__summary">
        <div className="stock-dashboard__stat-card stock-dashboard__stat-card--total">
          <div className="stock-dashboard__stat-label">Total Items</div>
          <div className="stock-dashboard__stat-value">{totalItems}</div>
          <div className="stock-dashboard__stat-sub">across all stores</div>
        </div>
        <div className="stock-dashboard__stat-card stock-dashboard__stat-card--stock">
          <div className="stock-dashboard__stat-label">Total Stock Units</div>
          <div className="stock-dashboard__stat-value">{totalStockValue.toLocaleString()}</div>
          <div className="stock-dashboard__stat-sub">combined inventory</div>
        </div>
        <div className="stock-dashboard__stat-card stock-dashboard__stat-card--healthy">
          <div className="stock-dashboard__stat-label">Healthy Stock</div>
          <div className="stock-dashboard__stat-value">{healthyStockCount}</div>
          <div className="stock-dashboard__stat-sub">above reorder level</div>
        </div>
        <div className="stock-dashboard__stat-card stock-dashboard__stat-card--alert">
          <div className="stock-dashboard__stat-label">Reorder Alerts</div>
          <div className="stock-dashboard__stat-value">{lowStockCount}</div>
          <div className="stock-dashboard__stat-sub">below reorder level</div>
        </div>
      </div>

      {/* Reorder alerts */}
      {reorderAlerts.length > 0 && (
        <div className="stock-dashboard__alerts">
          <h3 className="stock-dashboard__section-title">
            <span className="stock-dashboard__alert-icon">⚠</span>
            Reorder Alerts
          </h3>
          <div className="stock-dashboard__alerts-grid">
            {reorderAlerts.map((item) => {
              const severity = item.currentStock === 0 ? "critical" : item.currentStock <= item.reorderLevel / 2 ? "high" : "medium";
              return (
                <div key={item.id} className={`stock-alert stock-alert--${severity}`}>
                  <div className="stock-alert__info">
                    <span className="stock-alert__name">{item.name}</span>
                    <span className="stock-alert__code">{item.code}</span>
                  </div>
                  <div className="stock-alert__metrics">
                    <span className="stock-alert__current">{item.currentStock}</span>
                    <span className="stock-alert__separator">/</span>
                    <span className="stock-alert__reorder">≥ {item.reorderLevel}</span>
                  </div>
                  <span className={`badge badge--${severity === "critical" ? "error" : "warning"}`}>
                    {severity === "critical" ? "OUT OF STOCK" : severity === "high" ? "CRITICAL" : "LOW"}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Stock levels chart */}
      <div className="stock-dashboard__chart-card">
        <div className="stock-dashboard__chart-header">
          <h3 className="stock-dashboard__chart-title">Stock Levels Overview</h3>
          <HimsSelect
            value={selectedItemId}
            onChange={(e) => setSelectedItemId(Number(e.target.value))}
            aria-label="Select item for trend"
          >
            <option value={0}>Select item for trend...</option>
            {items.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name} ({item.code})
              </option>
            ))}
          </HimsSelect>
        </div>

        {selectedItemId > 0 && trend.length > 0 ? (
          <div className="stock-dashboard__trend">
            <h4 className="stock-dashboard__trend-title">14-Day Trend</h4>
            <StockTrendChart data={trend} color="#6366f1" />
            <div className="stock-dashboard__chart-stats">
              <div className="stock-dashboard__stat">
                <span className="stock-dashboard__stat-label">Latest</span>
                <span className="stock-dashboard__stat-value">{trend[trend.length - 1]?.closingStock ?? 0}</span>
              </div>
              <div className="stock-dashboard__stat">
                <span className="stock-dashboard__stat-label">Avg Inflow</span>
                <span className="stock-dashboard__stat-value stock-dashboard__stat-value--success">
                  {Math.round(trend.reduce((s, t) => s + t.receivedQty, 0) / trend.length)}
                </span>
              </div>
              <div className="stock-dashboard__stat">
                <span className="stock-dashboard__stat-label">Avg Outflow</span>
                <span className="stock-dashboard__stat-value stock-dashboard__stat-value--warning">
                  {Math.round(trend.reduce((s, t) => s + t.issuedQty, 0) / trend.length)}
                </span>
              </div>
            </div>
          </div>
        ) : (
          <div className="stock-dashboard__stock-bars">
            {items.slice(0, 8).map((item) => (
              <StockBar
                key={item.id}
                label={item.name}
                current={item.currentStock}
                max={item.maxStock}
                color={item.currentStock <= item.reorderLevel ? "#ef4444" : "#6366f1"}
              />
            ))}
          </div>
        )}
      </div>

      {/* Stock distribution table */}
      <div className="stock-dashboard__table-card">
        <h3 className="stock-dashboard__section-title">Stock Distribution</h3>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Item</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Category</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Current</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reorder Lvl</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Max</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {items.map((item) => (
              <Table.Row
                key={item.id}
              >
                <Table.Cell>{item.name}</Table.Cell>
                <Table.Cell>{item.code}</Table.Cell>
                <Table.Cell>{item.category ?? "—"}</Table.Cell>
                <Table.Cell>{item.currentStock}</Table.Cell>
                <Table.Cell>{item.reorderLevel}</Table.Cell>
                <Table.Cell>{item.maxStock}</Table.Cell>
                <Table.Cell>
                  {item.currentStock <= item.reorderLevel ? (
                    <span className="badge badge--warning">Reorder</span>
                  ) : (
                    <span className="badge badge--success">OK</span>
                  )}
                </Table.Cell>
              </Table.Row>
            ))}
            {items.length === 0 && (
              <Table.Row>
                <Table.Cell colSpan={7}>No items found.</Table.Cell>
              </Table.Row>
            )}
          </Table.Body>
        </Table.Root>
      </div>
    </div>
  );
}
