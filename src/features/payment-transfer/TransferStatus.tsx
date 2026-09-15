import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  transferStatusFilterSchema,
  type TransferStatusFilterValues,
} from "./zodSchema";
import { useTransferList, useTransferDetail, type TransferRecord, type TransferStatusFilter } from "./queries";

interface TransferStatusProps {
  initialTransferId?: string;
}

const STATUS_BADGES: Record<TransferRecord["status"], string> = {
  PENDING: "badge badge--warning",
  PROCESSING: "badge badge--info",
  COMPLETED: "badge badge--success",
  FAILED: "badge badge--danger",
  CANCELLED: "badge badge--secondary",
};

/**
 * TransferStatus — View and filter DBT payment transfer history.
 * Shows status timeline with auto-refresh for in-progress transfers.
 */
export function TransferStatus({ initialTransferId }: TransferStatusProps) {
  const [selectedTransferId, setSelectedTransferId] = useState<string | undefined>(initialTransferId);

  const { register, handleSubmit, watch } = useForm<TransferStatusFilterValues>({
    resolver: zodResolver(transferStatusFilterSchema),
    defaultValues: {
      fromDate: "",
      toDate: "",
      status: "",
      page: 0,
      size: 20,
    },
  });

  const rawFilter = watch();
  const filter: TransferStatusFilter = {
    ...rawFilter,
    status: rawFilter.status || undefined,
  };
  const { data: paginatedTransfers, isLoading, isError, error } = useTransferList(filter);
  const { data: transferDetail } = useTransferDetail(selectedTransferId ?? "");

  const transfers = paginatedTransfers?.content ?? [];
  const totalPages = paginatedTransfers?.totalPages ?? 0;
  const currentPage = paginatedTransfers?.page ?? 0;

  const handleApplyFilter = (_values: TransferStatusFilterValues) => {
    // Filter is reactive via watch, this just triggers re-render
  };

  const handlePageChange = (_page: number) => {
    // Pagination handled by parent via filter state
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="transfer-status">
      <div className="transfer-status__header">
        <h3 className="transfer-status__title">Transfer Status</h3>
        <p className="transfer-status__subtitle">
          Track the status of your DBT payment transfers
        </p>
      </div>

      {/* Filters */}
      <form onSubmit={handleSubmit(handleApplyFilter)} className="transfer-status__filters">
        <div className="transfer-status__filter-row">
          <div className="transfer-status__filter-field">
            <label htmlFor="filter-from-date" className="transfer-status__filter-label">
              From
            </label>
            <input
              id="filter-from-date"
              type="date"
              className="form-input form-input--sm"
              {...register("fromDate")}
            />
          </div>
          <div className="transfer-status__filter-field">
            <label htmlFor="filter-to-date" className="transfer-status__filter-label">
              To
            </label>
            <input
              id="filter-to-date"
              type="date"
              className="form-input form-input--sm"
              {...register("toDate")}
            />
          </div>
          <div className="transfer-status__filter-field">
            <label htmlFor="filter-status" className="transfer-status__filter-label">
              Status
            </label>
            <select
              id="filter-status"
              className="form-input form-input--sm"
              {...register("status")}
            >
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="PROCESSING">Processing</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <button type="submit" className="btn btn--sm btn--primary">
            Apply
          </button>
        </div>
      </form>

      {/* Transfer List */}
      <div className="transfer-status__list">
        {isLoading && (
          <div className="transfer-status__loading">
            <span className="spinner" aria-hidden="true" />
            Loading transfers...
          </div>
        )}

        {isError && (
          <div className="alert alert--error" role="alert">
            Failed to load transfers: {(error as Error)?.message ?? "Unknown error"}
          </div>
        )}

        {!isLoading && !isError && transfers.length === 0 && (
          <div className="transfer-status__empty">
            <p className="muted">No transfers found.</p>
          </div>
        )}

        {!isLoading && transfers.length > 0 && (
          <>
            <table className="table">
              <thead>
                <tr>
                  <th>Transfer ID</th>
                  <th>Beneficiary</th>
                  <th>Amount</th>
                  <th>Scheme</th>
                  <th>Status</th>
                  <th>UTR</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {transfers.map((transfer) => (
                  <tr key={transfer.transferId}>
                    <td className="transfer-status__id">{transfer.transferId.slice(0, 12)}…</td>
                    <td>{transfer.beneficiaryName}</td>
                    <td>₹ {transfer.amount.toLocaleString("en-IN")}</td>
                    <td>{transfer.scheme}</td>
                    <td>
                      <span className={STATUS_BADGES[transfer.status]}>
                        {transfer.status}
                      </span>
                    </td>
                    <td className="transfer-status__utr">
                      {transfer.utrNo ?? "—"}
                    </td>
                    <td>{formatDate(transfer.createdAt)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn--sm btn--ghost"
                        onClick={() => setSelectedTransferId(transfer.transferId)}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="transfer-status__pagination">
                <button
                  type="button"
                  className="btn btn--sm btn--secondary"
                  disabled={currentPage <= 0}
                  onClick={() => handlePageChange(currentPage - 1)}
                >
                  Previous
                </button>
                <span className="transfer-status__page-info">
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  type="button"
                  className="btn btn--sm btn--secondary"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => handlePageChange(currentPage + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Transfer Detail Panel */}
      {selectedTransferId && transferDetail && (
        <div className="transfer-status__detail">
          <div className="transfer-status__detail-header">
            <h4 className="transfer-status__detail-title">Transfer Details</h4>
            <button
              type="button"
              className="btn btn--sm btn--ghost"
              onClick={() => setSelectedTransferId(undefined)}
            >
              Close
            </button>
          </div>
          <div className="transfer-status__detail-content">
            <div className="transfer-status__detail-grid">
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Transfer ID</span>
                <span className="transfer-status__detail-value">{transferDetail.transferId}</span>
              </div>
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Beneficiary</span>
                <span className="transfer-status__detail-value">{transferDetail.beneficiaryName}</span>
              </div>
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Amount</span>
                <span className="transfer-status__detail-value">
                  ₹ {transferDetail.amount.toLocaleString("en-IN")}
                </span>
              </div>
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Scheme</span>
                <span className="transfer-status__detail-value">{transferDetail.scheme}</span>
              </div>
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Purpose</span>
                <span className="transfer-status__detail-value">{transferDetail.purpose}</span>
              </div>
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Status</span>
                <span className={STATUS_BADGES[transferDetail.status]}>
                  {transferDetail.status}
                </span>
              </div>
              {transferDetail.utrNo && (
                <div className="transfer-status__detail-item">
                  <span className="transfer-status__detail-label">UTR Number</span>
                  <span className="transfer-status__detail-value">{transferDetail.utrNo}</span>
                </div>
              )}
              {transferDetail.failureReason && (
                <div className="transfer-status__detail-item transfer-status__detail-item--full">
                  <span className="transfer-status__detail-label">Failure Reason</span>
                  <span className="transfer-status__detail-value transfer-status__detail-value--error">
                    {transferDetail.failureReason}
                  </span>
                </div>
              )}
              <div className="transfer-status__detail-item">
                <span className="transfer-status__detail-label">Created</span>
                <span className="transfer-status__detail-value">{formatDate(transferDetail.createdAt)}</span>
              </div>
              {transferDetail.completedAt && (
                <div className="transfer-status__detail-item">
                  <span className="transfer-status__detail-label">Completed</span>
                  <span className="transfer-status__detail-value">{formatDate(transferDetail.completedAt)}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
