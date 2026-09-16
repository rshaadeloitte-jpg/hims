import { useState } from "react";
import { useBillLedger, useRevenueAnalytics, useCollectionAnalytics } from "./hooks";
import { type LedgerEntry } from "./hooks";
import { Button, Table } from "@chakra-ui/react";

interface LedgerViewProps {
  billId?: number;
}

/**
 * LedgerView — payment ledger with analytics charts.
 * Vercel-style analytics: revenue trend, collection breakdown, daily bar chart.
 * When `billId` is provided, shows that bill's ledger. Otherwise shows
 * a summary dashboard with charts.
 */
export function LedgerView({ billId }: LedgerViewProps) {
  const { data: ledgerEntries = [], isLoading: ledgerLoading } = useBillLedger(billId ?? 0);
  const { data: revenueAnalytics } = useRevenueAnalytics("DAILY", 30);
  const { data: collectionAnalytics } = useCollectionAnalytics("DAILY", 30);

  const [view, setView] = useState<"ledger" | "charts">("ledger");

  if (ledgerLoading) {
    return (
      <div className="ledger-view">
        <div className="ledger-view__skeleton">
          <div className="skeleton skeleton--header" />
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="skeleton skeleton--row" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="ledger-view">
      <div className="ledger-view__header">
        <h2 className="ledger-view__title">
          {billId ? `Bill Ledger #${billId}` : "Payment Ledger"}
        </h2>
        <div className="ledger-view__toggle">
          <Button
            type="button"
            variant={view === "ledger" ? "primary" : "secondary"}
            className={`ledger-view__toggle-btn${view === "ledger" ? " ledger-view__toggle-btn--active" : ""}`}
            onClick={() => setView("ledger")}
          >
            Ledger
          </Button>
          <Button
            type="button"
            variant={view === "charts" ? "primary" : "secondary"}
            className={`ledger-view__toggle-btn${view === "charts" ? " ledger-view__toggle-btn--active" : ""}`}
            onClick={() => setView("charts")}
          >
            Analytics
          </Button>
        </div>
      </div>

      {view === "ledger" && (
        <LedgerTable entries={ledgerEntries} />
      )}

      {view === "charts" && (
        <div className="ledger-view__charts">
          {/* Revenue Trend — Vercel-style area chart mockup */}
          <div className="ledger-view__chart-card">
            <h3 className="ledger-view__chart-title">Revenue Trend (30 days)</h3>
            {revenueAnalytics && (
              <MiniBarChart
                labels={revenueAnalytics.labels.slice(-14)}
                values={revenueAnalytics.revenue.slice(-14)}
                color="#6366f1"
              />
            )}
            <div className="ledger-view__chart-stats">
              <div className="ledger-view__stat">
                <span className="ledger-view__stat-label">Total Revenue</span>
                <span className="ledger-view__stat-value">
                  ₹ {revenueAnalytics?.totalRevenue.toFixed(0) ?? "0"}
                </span>
              </div>
              <div className="ledger-view__stat">
                <span className="ledger-view__stat-label">Outstanding</span>
                <span className="ledger-view__stat-value ledger-view__stat-value--warning">
                  ₹ {revenueAnalytics?.totalOutstanding.toFixed(0) ?? "0"}
                </span>
              </div>
              <div className="ledger-view__stat">
                <span className="ledger-view__stat-label">Collections</span>
                <span className="ledger-view__stat-value ledger-view__stat-value--success">
                  ₹ {revenueAnalytics?.totalCollections.toFixed(0) ?? "0"}
                </span>
              </div>
            </div>
          </div>

          {/* Collection Breakdown */}
          <div className="ledger-view__chart-card">
            <h3 className="ledger-view__chart-title">Collection Breakdown</h3>
            {collectionAnalytics && (
              <MiniBarChart
                labels={collectionAnalytics.labels.slice(-14)}
                values={collectionAnalytics.collections.slice(-14)}
                color="#10b981"
              />
            )}
          </div>

          {/* Payment Method Breakdown */}
          <div className="ledger-view__chart-card">
            <h3 className="ledger-view__chart-title">By Payment Method</h3>
            <div className="ledger-view__method-breakdown">
              <MethodBar method="CASH" percent={45} amount={12500} color="#6366f1" />
              <MethodBar method="CARD" percent={25} amount={7000} color="#8b5cf6" />
              <MethodBar method="UPI" percent={20} amount={5600} color="#06b6d4" />
              <MethodBar method="CHEQUE" percent={10} amount={2800} color="#f59e0b" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function LedgerTable({ entries }: { entries: LedgerEntry[] }) {
  if (entries.length === 0) {
    return (
      <div className="ledger-view__empty">
        <p className="muted">No ledger entries found.</p>
      </div>
    );
  }

  return (
    <div className="ledger-view__table-wrap">
      <Table.Root size="sm" variant="line" className="ledger-view__table">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">Date</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">Type</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">Reference</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="ledger-view__col--amount">Debit</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="ledger-view__col--amount">Credit</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="ledger-view__col--amount">Balance</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {entries.map((entry) => {
            const isCredit = entry.entryType === "PAYMENT" || entry.entryType === "CREDIT_NOTE";
            return (
              <Table.Row key={entry.entryId}>
                <Table.Cell>{entry.entryDate}</Table.Cell>
                <Table.Cell>
                  <span className={`badge badge--ledger badge--${entry.entryType.toLowerCase()}`}>
                    {entry.entryType.replace("_", " ")}
                  </span>
                </Table.Cell>
                <Table.Cell>{entry.reference ?? "—"}</Table.Cell>
                <Table.Cell className="ledger-view__col--amount">
                  {!isCredit ? `₹ ${entry.amount.toFixed(2)}` : "—"}
                </Table.Cell>
                <Table.Cell className="ledger-view__col--amount">
                  {isCredit ? `₹ ${entry.amount.toFixed(2)}` : "—"}
                </Table.Cell>
                <Table.Cell className="ledger-view__col--amount ledger-view__balance">
                  ₹ {entry.runningBalance.toFixed(2)}
                </Table.Cell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table.Root>
    </div>
  );
}

function MiniBarChart({ labels, values, color }: { labels: string[]; values: number[]; color: string }) {
  const max = Math.max(...values, 1);
  const barWidth = 100 / values.length;

  return (
    <div className="mini-chart">
      <svg viewBox={`0 0 ${values.length * 20} 100`} className="mini-chart__svg" preserveAspectRatio="none">
        {values.map((v, i) => {
          const height = (v / max) * 80;
          return (
            <rect
              key={i}
              x={i * 20 + 2}
              y={100 - height - 10}
              width={barWidth - 4}
              height={height}
              fill={color}
              rx={2}
              opacity={0.85}
            >
              <title>{labels[i]}: ₹{v.toFixed(0)}</title>
            </rect>
          );
        })}
      </svg>
      <div className="mini-chart__labels">
        {labels.map((l, i) => (
          <span key={i} className="mini-chart__label">
            {l.slice(5)}
          </span>
        ))}
      </div>
    </div>
  );
}

function MethodBar({ method, percent, amount, color }: { method: string; percent: number; amount: number; color: string }) {
  return (
    <div className="method-bar">
      <div className="method-bar__header">
        <span className="method-bar__method">{method}</span>
        <span className="method-bar__amount">₹ {amount.toLocaleString()}</span>
      </div>
      <div className="method-bar__track">
        <div
          className="method-bar__fill"
          style={{ width: `${percent}%`, backgroundColor: color }}
        />
      </div>
      <span className="method-bar__percent">{percent}%</span>
    </div>
  );
}
