import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { Button } from "@chakra-ui/react";
import {
  normalizeAppointment,
  useAppointment,
  useAppointmentList,
  useCancelAppointment,
  useCheckInAppointment,
  useCompleteAppointment,
  useDepartments,
  useDoctorsMaster,
  useNoShowAppointment,
  usePatients,
} from "../api/queries";
import { DataTable, type DataTableColumn } from "../components/DataTable";
import { AppointmentStatusBadge } from "../components/StatusBadges";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { PatientBanner } from "../components/PatientBanner";
import { usePatient } from "../api/usePatient";
import type {
  Appointment,
  AppointmentListResponse,
  AppointmentStatus,
} from "../api/types";
import type { ApiError } from "../../../api/client";

/**
 * AppointmentListPage — legacy `appointmentList:showAppointments` mapped.
 *
 * The 09 §2.1 spec lists 10+ category badges (All/CheckedOut/CheckedIn/
 * Pending/NoShow/Walkins/Emergency/Online/Teleconsult/FPV/RGHS). We ship the
 * six pinned by the existing API surface (status + source filters) and the
 * per-category "checked in / pending / no-show" badges on each row. The
 * remaining sources (teleconsult/FPV/RGHS) land when the corresponding
 * ScheduleBoard / RGHS bridges are cut over to React.
 *
 * Pagination follows the legacy /v1/appointments cursor: page + size, with
 * `hasMore` driving the Next button (no total-count column to depend on).
 */
const PAGE_SIZE = 25;

type StatusFilter = "all" | "checkedIn" | "pending" | "noShow" | "walkIn" | "emergency";

const STATUS_FILTERS: { key: StatusFilter; label: string; predicate: (a: Appointment) => boolean }[] = [
  { key: "all", label: "All", predicate: () => true },
  {
    key: "pending",
    label: "Pending",
    predicate: (a) => (a.apptStatus ?? a.status) === 1,
  },
  {
    key: "checkedIn",
    label: "Checked-In",
    predicate: (a) => (a.apptStatus ?? a.status) === 2,
  },
  {
    key: "noShow",
    label: "No-Show",
    predicate: (a) => (a.apptStatus ?? a.status) === 6,
  },
  {
    key: "walkIn",
    label: "Walk-in",
    predicate: (a) =>
      a.source === "WALK_IN" ||
      (typeof a.visitType === "string" && a.visitType.toUpperCase() === "WALK_IN"),
  },
  {
    key: "emergency",
    label: "Emergency",
    predicate: (a) =>
      a.source === "EMERGENCY" ||
      (typeof a.visitType === "string" && a.visitType.toUpperCase() === "EMERGENCY"),
  },
];

export function AppointmentListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [filter, setFilter] = useState<StatusFilter>((searchParams.get("filter") as StatusFilter) || "all");
  const [page, setPage] = useState<number>(Number(searchParams.get("page") || "0") || 0);
  const [date] = useState<string>(searchParams.get("date") || new Date().toISOString().slice(0, 10));
  const highlightParam = searchParams.get("highlight");
  const [highlight] = useState<number | null>(
    highlightParam ? Number(highlightParam) : null,
  );
  const [selected, setSelected] = useState<Appointment | null>(null);
  const [cancelReason, setCancelReason] = useState<string>("");
  const [showCancel, setShowCancel] = useState<boolean>(false);
  const [showReschedule, setShowReschedule] = useState<boolean>(false);
  const [rescheduleReason, setRescheduleReason] = useState<string>("");
  const [actionError, setActionError] = useState<string | null>(null);

  const list = useAppointmentList({ date, status: filter === "all" ? "all" : filter, page, size: PAGE_SIZE });
  const cancel = useCancelAppointment();
  const noShow = useNoShowAppointment();
  const checkIn = useCheckInAppointment();
  const complete = useCompleteAppointment();

  const rawList = list.data;
  const rawItems: unknown[] = Array.isArray(rawList)
    ? rawList
    : (rawList as AppointmentListResponse | undefined)?.items ?? [];

  const normalizedItems = useMemo(() => rawItems.map(normalizeAppointment), [rawItems]);

  const patientIds = useMemo(() => {
    const ids = new Set<number>();
    for (const item of normalizedItems) {
      if (typeof item.patientId === "number" && item.patientId > 0) {
        ids.add(item.patientId);
      }
    }
    return Array.from(ids);
  }, [normalizedItems]);

  const patientQueries = usePatients(patientIds);
  const departmentsQuery = useDepartments(null);
  const doctorsQuery = useDoctorsMaster(null);

  const patientMap = useMemo(() => {
    const map = new Map<number, { name: string; mrn?: string }>();
    patientQueries.forEach((q, idx) => {
      const p = q.data;
      if (p) {
        const fullName = [p.firstName, p.middleName, p.lastName].filter(Boolean).join(" ");
        const name = fullName.trim() || p.firstName || `#${p.id}`;
        map.set(patientIds[idx], { name, mrn: p.mrn });
      }
    });
    return map;
  }, [patientQueries, patientIds]);

  const doctorMap = useMemo(() => {
    const map = new Map<number, string>();
    const docs = doctorsQuery.data ?? [];
    for (const d of docs) {
      const id = Number(d.id);
      if (!id) continue;
      const fullName = [d.firstName, d.lastName].filter(Boolean).join(" ");
      const name = fullName
        ? (fullName.startsWith("Dr.") ? fullName : `Dr. ${fullName}`)
        : (d.name ? String(d.name) : `Doctor #${id}`);
      map.set(id, name);
    }
    return map;
  }, [doctorsQuery.data]);

  const departmentMap = useMemo(() => {
    const map = new Map<number, string>();
    const depts = departmentsQuery.data ?? [];
    for (const d of depts) {
      if (d.id != null) {
        const name = d.description || ((d as unknown as Record<string, unknown>).name as string) || d.code || `#${d.id}`;
        map.set(d.id, name);
      }
    }
    return map;
  }, [departmentsQuery.data]);

  const enrichedItems: Appointment[] = useMemo(() => {
    return normalizedItems.map((a) => {
      const pInfo = patientMap.get(a.patientId);
      const docName = a.doctorName ?? doctorMap.get(a.doctorId);
      const dId = a.departmentId ?? a.deptId;
      const deptName = a.departmentName ?? (dId != null ? departmentMap.get(dId) : undefined);
      return {
        ...a,
        patientName: a.patientName ?? pInfo?.name,
        patientMrn: a.patientMrn ?? pInfo?.mrn,
        doctorName: docName,
        departmentName: deptName,
      };
    });
  }, [normalizedItems, patientMap, doctorMap, departmentMap]);

  const visible: Appointment[] = useMemo(() => {
    const f = STATUS_FILTERS.find((s) => s.key === filter);
    return f ? enrichedItems.filter(f.predicate) : enrichedItems;
  }, [enrichedItems, filter]);

  const total: number = Array.isArray(rawList)
    ? visible.length
    : (rawList as AppointmentListResponse | undefined)?.total ?? visible.length;
  const hasMore: boolean =
    (rawList as AppointmentListResponse | undefined)?.hasMore ?? visible.length >= PAGE_SIZE;

  // Highlight card data:
  const appointmentInList = useMemo(
    () => (highlight != null ? enrichedItems.find((a) => a.id === highlight) : undefined),
    [enrichedItems, highlight],
  );
  const { data: fetchedApptRaw } = useAppointment(highlight ?? undefined);
  const highlightedAppointment = useMemo(() => {
    if (appointmentInList) return appointmentInList;
    if (fetchedApptRaw) return normalizeAppointment(fetchedApptRaw);
    return null;
  }, [appointmentInList, fetchedApptRaw]);

  const effectivePatientId = highlightedAppointment?.patientId;
  const { patient: highlightedPatient } = usePatient(effectivePatientId ?? undefined);

  const handleCheckIn = async (id: number) => {
    setActionError(null);
    try {
      await checkIn.mutateAsync(id);
      await list.refetch();
    } catch (err: unknown) {
      const message = (err as ApiError)?.message || (err as Error)?.message || "Check-in failed";
      setActionError(message);
    }
  };

  const handleNoShow = async (id: number) => {
    setActionError(null);
    try {
      await noShow.mutateAsync(id);
      await list.refetch();
    } catch (err: unknown) {
      const message = (err as ApiError)?.message || (err as Error)?.message || "No-show failed";
      setActionError(message);
    }
  };

  const handleComplete = async (id: number) => {
    setActionError(null);
    try {
      await complete.mutateAsync(id);
      await list.refetch();
    } catch (err: unknown) {
      const message = (err as ApiError)?.message || (err as Error)?.message || "Complete failed";
      setActionError(message);
    }
  };

  const handleCancelConfirm = async () => {
    if (!selected) return;
    setActionError(null);
    try {
      await cancel.mutateAsync({ id: selected.id, reason: cancelReason.trim() });
      setShowCancel(false);
      setCancelReason("");
      setSelected(null);
      await list.refetch();
    } catch (err: unknown) {
      const message = (err as ApiError)?.message || (err as Error)?.message || "Cancellation failed";
      setActionError(message);
      setShowCancel(false);
      setCancelReason("");
      setSelected(null);
    }
  };

  const setFilterAndPage = (next: StatusFilter, nextPage: number) => {
    setActionError(null);
    setFilter(next);
    setPage(nextPage);
    const params = new URLSearchParams(searchParams);
    if (next === "all") params.delete("filter");
    else params.set("filter", next);
    if (nextPage === 0) params.delete("page");
    else params.set("page", String(nextPage));
    setSearchParams(params, { replace: true });
  };

  const columns: DataTableColumn<Appointment>[] = [
    {
      key: "id",
      header: "#",
      width: "6ch",
      accessor: (a) => a.id,
      render: (a) => <span className="muted">#{a.id}</span>,
    },
    {
      key: "time",
      header: "When",
      accessor: (a) => a.startAt || a.apptDate || "",
      render: (a) => {
        let formattedDate = a.apptDate ?? "—";
        const dateStr = a.startAt || (a.apptDate ? `${a.apptDate}${a.slotStart ? `T${a.slotStart}` : ""}` : "");
        if (dateStr) {
          try {
            const d = new Date(dateStr);
            if (!isNaN(d.getTime())) {
              formattedDate = d.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
            }
          } catch {
            // fallback
          }
        }
        let endStr: string | null = null;
        if (a.endAt) {
          try {
            const d = new Date(a.endAt);
            if (!isNaN(d.getTime())) {
              endStr = d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
            }
          } catch {}
        } else if (a.slotEnd) {
          endStr = a.slotEnd;
        }
        return (
          <div>
            <div>{formattedDate}</div>
            {endStr ? <div className="muted small">to {endStr}</div> : null}
          </div>
        );
      },
    },
    {
      key: "patient",
      header: "Patient",
      accessor: (a) => a.patientName ?? (a.patientId ? `#${a.patientId}` : "—"),
      render: (a) => (
        <div>
          <div>{a.patientName ?? (a.patientId ? `#${a.patientId}` : "—")}</div>
          {a.patientMrn ? <div className="muted small">{a.patientMrn}</div> : null}
        </div>
      ),
    },
    {
      key: "doctor",
      header: "Doctor",
      accessor: (a) => a.doctorName ?? (a.doctorId ? `#${a.doctorId}` : "—"),
    },
    {
      key: "department",
      header: "Dept",
      accessor: (a) => a.departmentName ?? (a.departmentId != null ? `#${a.departmentId}` : (a.deptId != null ? `#${a.deptId}` : "—")),
    },
    {
      key: "source",
      header: "Source",
      accessor: (a) => a.source ?? a.visitType ?? "—",
      render: (a) => <span className="badge badge--muted">{a.source ?? a.visitType ?? "—"}</span>,
    },
    {
      key: "token",
      header: "Token",
      accessor: (a) => a.tokenNumber ?? (a.tokenNo != null ? String(a.tokenNo) : ""),
      render: (a) => (a.tokenNumber || a.tokenNo != null) ? <code>{a.tokenNumber ?? a.tokenNo}</code> : <span className="muted">—</span>,
    },
    {
      key: "status",
      header: "Status",
      accessor: (a) => a.apptStatus ?? a.status,
      render: (a) => <AppointmentStatusBadge status={(a.apptStatus ?? a.status) as AppointmentStatus} />,
    },
    {
      key: "actions",
      header: "",
      sortable: false,
      accessor: () => null,
      render: (a) => (
        <div className="row-actions">
          {(a.apptStatus ?? a.status) === 1 ? (
            <>
              <Button
                type="button"
                variant="primary"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  void handleCheckIn(a.id);
                }}
                disabled={checkIn.isPending}
                loading={checkIn.isPending}
              >
                Check in
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  setSelected(a);
                  setShowReschedule(true);
                }}
              >
                Reschedule
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  setSelected(a);
                  setShowCancel(true);
                }}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  void handleNoShow(a.id);
                }}
                disabled={noShow.isPending}
                loading={noShow.isPending}
              >
                No-show
              </Button>
            </>
          ) : (a.apptStatus ?? a.status) === 2 ? (
            <Button
              type="button"
              variant="primary"
              size="sm"
              onClick={(e) => {
                e.stopPropagation();
                void handleComplete(a.id);
              }}
              disabled={complete.isPending}
              loading={complete.isPending}
            >
              Complete
            </Button>
          ) : null}
        </div>
      ),
    },
  ];

  return (
    <div className="page page--appt-list">
      <header className="page-header">
        <div>
          <h1>Appointments</h1>
          <p className="muted">
            {total} matching · page {page + 1}{hasMore ? "+" : ""}
          </p>
        </div>
        <Button
          type="button"
          variant="primary"
          onClick={() => navigate("/m/op/addAppointment")}
        >
          + New appointment
        </Button>
      </header>

      {highlight != null && (highlightedPatient || highlightedAppointment) ? (
        <div className="card card--banner" data-testid="highlight-card">
          {highlightedPatient ? (
            <PatientBanner patient={highlightedPatient} />
          ) : (
            <div className="patient-banner">
              <div className="patient-banner__bar" aria-hidden="true" />
              <div className="patient-banner__content">
                <div className="patient-banner__id">
                  <span className="patient-banner__mrn">
                    {highlightedAppointment?.patientMrn ??
                      (highlightedAppointment?.patientId ? `Patient #${highlightedAppointment.patientId}` : "")}
                  </span>
                  <span className="patient-banner__name">
                    {highlightedAppointment?.patientName ??
                      (highlightedAppointment?.patientId
                        ? `Patient #${highlightedAppointment.patientId}`
                        : `Appointment #${highlight}`)}
                  </span>
                </div>
              </div>
            </div>
          )}
          <p className="muted small">
            Just created appointment #{highlight}
            {highlightedAppointment?.tokenNumber
              ? ` · Token ${highlightedAppointment.tokenNumber}`
              : highlightedAppointment?.tokenNo != null
                ? ` · Token ${highlightedAppointment.tokenNo}`
                : ""}
            {highlightedAppointment?.apptDate ? ` · Date ${highlightedAppointment.apptDate}` : ""}
            {highlightedAppointment?.slotStart ? ` · Time ${highlightedAppointment.slotStart}` : ""}.
            It will appear in the list below as soon as the refetch resolves.
          </p>
        </div>
      ) : null}

      <nav className="filter-strip" aria-label="Filter by status">
        {STATUS_FILTERS.map((f) => {
          const active = filter === f.key;
          return (
            <Button
              key={f.key}
              type="button"
              variant={active ? "primary" : "secondary"}
              size="sm"
              aria-pressed={active}
              onClick={() => setFilterAndPage(f.key, 0)}
            >
              {f.label}
            </Button>
          );
        })}
      </nav>

      <section className="card">
        {actionError ? (
          <p className="alert alert--error" role="alert">
            {actionError}
          </p>
        ) : null}
        {list.isError ? (
          <p className="alert alert--error" role="alert">
            Could not load appointments: {String((list.error as Error)?.message ?? list.error)}
          </p>
        ) : null}
        <DataTable
          rows={visible}
          columns={columns}
          rowKey={(a) => a.id}
          loading={list.isLoading}
          ariaLabel="Appointments"
          emptyState={
            !list.isError && !list.isLoading && visible.length === 0 ? (
              <div className="empty-state">
                <p>No appointments match this filter.</p>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => setFilterAndPage("all", 0)}
                >
                  Clear filter
                </Button>
              </div>
            ) : undefined
          }
        />

        <div className="pagination">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={page === 0}
            onClick={() => setFilterAndPage(filter, Math.max(0, page - 1))}
          >
            ← Prev
          </Button>
          <span className="pagination-page">Page {page + 1}</span>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={!hasMore}
            onClick={() => setFilterAndPage(filter, page + 1)}
          >
            Next →
          </Button>
        </div>
      </section>

      <ConfirmDialog
        open={showCancel}
        title="Cancel appointment"
        body={
          selected ? (
            <p>
              Are you sure you want to cancel appointment <strong>#{selected.id}</strong> for{" "}
              <strong>{selected.patientName ?? `#${selected.patientId}`}</strong> on{" "}
              {new Date(selected.startAt).toLocaleString()}?
            </p>
          ) : null
        }
        reasonField={{
          label: "Reason for cancellation",
          placeholder: "Patient request, doctor unavailable, …",
          required: true,
          value: cancelReason,
          onChange: setCancelReason,
        }}
        confirmLabel="Cancel appointment"
        confirmTone="danger"
        busy={cancel.isPending}
        onCancel={() => {
          setShowCancel(false);
          setCancelReason("");
          setSelected(null);
        }}
        onConfirm={() => {
          void handleCancelConfirm();
        }}
      />

      <ConfirmDialog
        open={showReschedule}
        title="Reschedule appointment"
        body={
          selected ? (
            <p>
              Reschedule appointment <strong>#{selected.id}</strong> for{" "}
              <strong>{selected.patientName ?? `#${selected.patientId}`}</strong>. The legacy
              flow retires the current slot to status 5 (RESCHEDULED) and creates a linked
              appointment; in this React build, you can capture a reschedule reason here and
              follow up via the appointment screen.
            </p>
          ) : null
        }
        reasonField={{
          label: "Reschedule reason",
          placeholder: "Doctor unavailable, patient request, equipment issue, …",
          required: true,
          value: rescheduleReason,
          onChange: setRescheduleReason,
        }}
        confirmLabel="Mark for reschedule"
        busy={false}
        onCancel={() => {
          setShowReschedule(false);
          setRescheduleReason("");
          setSelected(null);
        }}
        onConfirm={() => {
          setShowReschedule(false);
          setRescheduleReason("");
          setSelected(null);
        }}
      />
    </div>
  );
}
