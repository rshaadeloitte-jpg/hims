import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  bankReconciliationFilterSchema,
  bankMatchSchema,
  type BankReconciliationFilterValues,
  type BankMatchValues,
} from "./zodSchema";

interface BankReconciliationProps {
  onImported?: (statementId: number) => void;
}

interface StatementRow {
  lineId: number;
  transactionDate: string;
  description: string;
  referenceNo?: string;
  debitAmount: number;
  creditAmount: number;
  balance?: number;
  matchStatus: "UNMATCHED" | "MATCHED" | "DISPUTED";
  matchedJournalId?: number;
}

interface JournalMatch {
  journalId: number;
  voucherNo: string;
  entryDate: string;
  narration: string;
  amount: number;
}

const BANK_ACCOUNTS = [
  { id: 1, name: "Bank - SBI Current", accountNo: "XXXX XXXX 1234" },
  { id: 2, name: "Bank - HDFC Savings", accountNo: "XXXX XXXX 5678" },
  { id: 3, name: "Cash in Hand", accountNo: "PETTY-CASH" },
];

/**
 * BankReconciliation — import statement lines and match against journal entries.
 *
 * UX (Stripe table pattern):
 *   • Select bank account + date range → fetch statement + unmatched journals
 *   • Side-by-side or tabbed: Statement lines | Unmatched Journal entries
 *   • Click a statement row → see suggested matches → confirm or dispute
 *   • Bulk match: auto-match by reference/amount heuristic
 */
export function BankReconciliation({ onImported }: BankReconciliationProps) {
  const [selectedAccount, setSelectedAccount] = useState<number>(0);
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [statementRows, setStatementRows] = useState<StatementRow[]>([]);
  const [unmatchedJournals, setUnmatchedJournals] = useState<JournalMatch[]>([]);
  const [selectedRow, setSelectedRow] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BankReconciliationFilterValues>({
    resolver: zodResolver(bankReconciliationFilterSchema),
    defaultValues: {
      status: "ALL",
      page: 0,
      size: 50,
    },
  });

  const loadReconciliation = async () => {
    if (!selectedAccount || !fromDate || !toDate) return;
    setIsLoading(true);
    try {
      // In production: replace with useBankReconciliation query
      const resp = await fetch(
        `/api/v1/bank/reconciliation?bankAccountId=${selectedAccount}&fromDate=${fromDate}&toDate=${toDate}`,
      );
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const data = await resp.json();
      setStatementRows(data.lines ?? []);
      setUnmatchedJournals(data.unmatchedJournals ?? []);
    } catch (err) {
      console.error("Failed to load reconciliation:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleImportStatement = async () => {
    if (!importFile || !selectedAccount) return;
    setIsImporting(true);
    try {
      // Parse CSV/XLSX in production; here we read as text for demo
      const text = await importFile.text();
      const lines = text.split("\n").slice(1); // skip header
      const parsed = lines
        .filter((l) => l.trim())
        .map((line, idx) => {
          const cols = line.split(",");
          return {
            lineId: idx + 1,
            transactionDate: cols[0]?.trim() ?? "",
            description: cols[1]?.trim() ?? "",
            referenceNo: cols[2]?.trim(),
            debitAmount: parseFloat(cols[3] ?? "0") || 0,
            creditAmount: parseFloat(cols[4] ?? "0") || 0,
            matchStatus: "UNMATCHED" as const,
          };
        });

      setStatementRows(parsed);

      // POST to backend
      const resp = await fetch("/api/v1/bank/reconciliation/statement", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bankAccountId: selectedAccount, lines: parsed }),
      });
      if (resp.ok) {
        const data = await resp.json();
        onImported?.(data.statementId);
      }
    } catch (err) {
      console.error("Import failed:", err);
    } finally {
      setIsImporting(false);
    }
  };

  const handleMatch = async (row: StatementRow, journalId: number) => {
    try {
      await fetch("/api/v1/bank/reconciliation/match", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          statementLineId: row.lineId,
          journalEntryId: journalId,
          matchAmount: row.debitAmount || row.creditAmount,
        }),
      });
      // Update local state
      setStatementRows((prev) =>
        prev.map((r) =>
          r.lineId === row.lineId
            ? { ...r, matchStatus: "MATCHED", matchedJournalId: journalId }
            : r,
        ),
      );
      setSelectedRow(null);
    } catch (err) {
      console.error("Match failed:", err);
    }
  };

  const handleDispute = async (row: StatementRow) => {
    try {
      await fetch("/api/v1/bank/reconciliation/dispute", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ statementLineId: row.lineId }),
      });
      setStatementRows((prev) =>
        prev.map((r) => (r.lineId === row.lineId ? { ...r, matchStatus: "DISPUTED" } : r)),
      );
      setSelectedRow(null);
    } catch (err) {
      console.error("Dispute failed:", err);
    }
  };

  // Auto-match heuristic: same reference number or exact amount
  const handleAutoMatch = () => {
    let matched = 0;
    setStatementRows((prev) =>
      prev.map((row) => {
        if (row.matchStatus === "UNMATCHED") {
          const match = unmatchedJournals.find(
            (j) =>
              (row.referenceNo && j.voucherNo.includes(row.referenceNo)) ||
              j.amount === row.debitAmount ||
              j.amount === row.creditAmount,
          );
          if (match) {
            matched++;
            return { ...row, matchStatus: "MATCHED" as const, matchedJournalId: match.journalId };
          }
        }
        return row;
      }),
    );
    console.log(`Auto-matched ${matched} rows`);
  };

  const matchedCount = statementRows.filter((r) => r.matchStatus === "MATCHED").length;
  const totalCount = statementRows.length;
  const unmatchedCount = totalCount - matchedCount;

  return (
    <div className="rmrs-bank-recon">
      <div className="page-header">
        <h1>Bank Reconciliation</h1>
        <p className="text-sm text-gray-500">
          Import statements and match against journal entries
        </p>
      </div>

      {/* Filter bar */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="form-label">Bank Account</label>
            <select
              className="form-input"
              value={selectedAccount}
              onChange={(e) => setSelectedAccount(Number(e.target.value))}
            >
              <option value={0}>Select account...</option>
              {BANK_ACCOUNTS.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} ({acc.accountNo})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label" htmlFor="fromDate">
              From Date
            </label>
            <input
              id="fromDate"
              type="date"
              className="form-input"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>

          <div>
            <label className="form-label" htmlFor="toDate">
              To Date
            </label>
            <input
              id="toDate"
              type="date"
              className="form-input"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>

          <div className="flex items-end">
            <button
              type="button"
              className="btn btn--primary"
              onClick={loadReconciliation}
              disabled={!selectedAccount || !fromDate || !toDate || isLoading}
            >
              {isLoading ? "Loading..." : "Load"}
            </button>
          </div>
        </div>
      </div>

      {/* Import section */}
      <div className="card">
        <h2 className="text-lg font-semibold mb-3">Import Statement</h2>
        <div className="flex gap-3 items-end">
          <div className="flex-1">
            <label className="form-label">Upload CSV/XLSX</label>
            <input
              type="file"
              accept=".csv,.xlsx,.xls"
              className="form-input"
              onChange={(e) => setImportFile(e.target.files?.[0] ?? null)}
            />
          </div>
          <button
            type="button"
            className="btn btn--secondary"
            onClick={handleImportStatement}
            disabled={!importFile || !selectedAccount || isImporting}
          >
            {isImporting ? "Importing..." : "Import"}
          </button>
        </div>
      </div>

      {/* Progress bar */}
      {totalCount > 0 && (
        <div className="card">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium">Reconciliation Progress</span>
            <span className="text-sm text-gray-500">
              {matchedCount} / {totalCount} matched
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2.5">
            <div
              className="bg-green-600 h-2.5 rounded-full transition-all"
              style={{ width: `${totalCount > 0 ? (matchedCount / totalCount) * 100 : 0}%` }}
            />
          </div>
          <div className="flex gap-4 mt-2 text-xs text-gray-600">
            <span className="text-green-700">Matched: {matchedCount}</span>
            <span className="text-amber-700">Unmatched: {unmatchedCount}</span>
            <span className="text-red-700">
              Disputed: {statementRows.filter((r) => r.matchStatus === "DISPUTED").length}
            </span>
          </div>
          <button
            type="button"
            className="btn btn--sm btn--ghost mt-2"
            onClick={handleAutoMatch}
          >
            Auto-match by reference/amount
          </button>
        </div>
      )}

      {/* Statement table */}
      {statementRows.length > 0 && (
        <div className="card">
          <h2 className="text-lg font-semibold mb-3">Statement Lines</h2>
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Reference</th>
                  <th className="text-right">Debit</th>
                  <th className="text-right">Credit</th>
                  <th className="text-right">Balance</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {statementRows.map((row) => (
                  <tr
                    key={row.lineId}
                    className={`${
                      row.matchStatus === "MATCHED"
                        ? "bg-green-50"
                        : row.matchStatus === "DISPUTED"
                        ? "bg-red-50"
                        : ""
                    } ${selectedRow === row.lineId ? "ring-2 ring-blue-300" : ""}`}
                  >
                    <td>{row.transactionDate}</td>
                    <td>{row.description}</td>
                    <td className="font-mono text-xs">{row.referenceNo ?? "—"}</td>
                    <td className="text-right font-mono">
                      {row.debitAmount > 0 ? `₹${row.debitAmount.toFixed(2)}` : "—"}
                    </td>
                    <td className="text-right font-mono">
                      {row.creditAmount > 0 ? `₹${row.creditAmount.toFixed(2)}` : "—"}
                    </td>
                    <td className="text-right font-mono">
                      {row.balance != null ? `₹${row.balance.toFixed(2)}` : "—"}
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          row.matchStatus === "MATCHED"
                            ? "badge--success"
                            : row.matchStatus === "DISPUTED"
                            ? "badge--danger"
                            : "badge--warning"
                        }`}
                      >
                        {row.matchStatus}
                      </span>
                    </td>
                    <td>
                      {row.matchStatus === "UNMATCHED" && (
                        <div className="flex gap-1">
                          <button
                            type="button"
                            className="btn btn--xs btn--primary"
                            onClick={() => setSelectedRow(row.lineId)}
                          >
                            Match
                          </button>
                          <button
                            type="button"
                            className="btn btn--xs btn--danger"
                            onClick={() => handleDispute(row)}
                          >
                            Dispute
                          </button>
                        </div>
                      )}
                      {row.matchStatus === "MATCHED" && row.matchedJournalId && (
                        <span className="text-xs text-green-700">
                          ✓ #{row.matchedJournalId}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Match panel */}
      {selectedRow != null && (
        <MatchPanel
          row={statementRows.find((r) => r.lineId === selectedRow)!}
          journals={unmatchedJournals}
          onMatch={handleMatch}
          onClose={() => setSelectedRow(null)}
        />
      )}

      {errors.bankAccountId && (
        <div className="alert alert--error">{errors.bankAccountId.message}</div>
      )}
    </div>
  );
}

/** Slide-in panel to match a statement row to a journal entry. */
function MatchPanel({
  row,
  journals,
  onMatch,
  onClose,
}: {
  row: StatementRow;
  journals: JournalMatch[];
  onMatch: (row: StatementRow, journalId: number) => void;
  onClose: () => void;
}) {
  const [search, setSearch] = useState("");
  const amount = row.debitAmount || row.creditAmount;

  // Suggest matches: by reference, then by amount similarity
  const suggested = journals.filter((j) => {
    const matchesRef =
      row.referenceNo && j.voucherNo.includes(row.referenceNo);
    const matchesAmount = Math.abs(j.amount - amount) < 0.01;
    return matchesRef || matchesAmount;
  });

  const other = journals.filter((j) => !suggested.includes(j));
  const filtered = [...suggested, ...other].filter(
    (j) =>
      !search ||
      j.voucherNo.toLowerCase().includes(search.toLowerCase()) ||
      j.narration.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className="card border border-blue-200 bg-blue-50">
      <div className="flex justify-between items-center mb-3">
        <h3 className="font-semibold">
          Match Statement Line → Journal Entry
        </h3>
        <button type="button" className="btn btn--xs btn--ghost" onClick={onClose}>
          ✕
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
        <div>
          <p className="text-gray-500">Statement Line</p>
          <p className="font-medium">{row.description}</p>
          <p className="font-mono text-xs">
            {row.transactionDate} · {row.referenceNo ?? "No ref"}
          </p>
        </div>
        <div className="text-right">
          <p className="text-gray-500">Amount</p>
          <p className="font-mono font-bold text-lg">₹{amount.toFixed(2)}</p>
          <p className="text-xs text-gray-500">
            {row.debitAmount > 0 ? "Debit" : "Credit"}
          </p>
        </div>
      </div>

      <div className="mb-3">
        <input
          type="text"
          className="form-input"
          placeholder="Search journal entries by voucher no or narration..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {suggested.length > 0 && (
        <div className="mb-3">
          <p className="text-xs font-medium text-blue-700 mb-1">
            Suggested Matches ({suggested.length})
          </p>
          <div className="space-y-1">
            {suggested.slice(0, 3).map((j) => (
              <button
                key={j.journalId}
                type="button"
                className="w-full text-left p-2 bg-white rounded border border-blue-200 hover:border-blue-400 transition-colors"
                onClick={() => onMatch(row, j.journalId)}
              >
                <div className="flex justify-between">
                  <span className="font-medium">{j.voucherNo}</span>
                  <span className="font-mono text-green-700">₹{j.amount.toFixed(2)}</span>
                </div>
                <p className="text-xs text-gray-500">{j.narration}</p>
                <p className="text-xs text-gray-400">{j.entryDate}</p>
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="max-h-40 overflow-y-auto space-y-1">
        {filtered.slice(0, 10).map((j) => (
          <button
            key={j.journalId}
            type="button"
            className="w-full text-left p-2 bg-white rounded border border-gray-200 hover:border-blue-300"
            onClick={() => onMatch(row, j.journalId)}
          >
            <div className="flex justify-between text-sm">
              <span>{j.voucherNo}</span>
              <span className="font-mono">₹{j.amount.toFixed(2)}</span>
            </div>
            <p className="text-xs text-gray-500 truncate">{j.narration}</p>
          </button>
        ))}
        {filtered.length === 0 && (
          <p className="text-sm text-gray-500 text-center py-3">
            No matching journal entries found.
          </p>
        )}
      </div>
    </div>
  );
}
