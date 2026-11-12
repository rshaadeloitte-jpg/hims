import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../../components/ui";
import {
  useCreateSchedule,
  useDepartments,
  useDoctorScheduleList,
  useDoctorsMaster,
} from "../api/queries";
import { useOpSession } from "../store/opSession";
import { DataTable, type DataTableColumn } from "../components/DataTable";
import type { DoctorSchedule } from "../api/types";
import type { ApiError } from "../../../api/client";

const DAY_NAMES = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

const scheduleSchema = z
  .object({
    doctorId: z.coerce.number().int().positive("Doctor is required"),
    departmentId: z.preprocess(
      (v) => (v === "" || v == null ? undefined : v),
      z.coerce.number().optional(),
    ),
    dayOfWeek: z.coerce.number().int().min(1, "Day is required").max(7, "Day is required"),
    startTime: z.string().regex(/^\d{2}:\d{2}(:\d{2})?$/, "Start time is required"),
    endTime: z.string().regex(/^\d{2}:\d{2}(:\d{2})?$/, "End time is required"),
    slotMinutes: z.coerce.number().int().min(5, "Min 5").max(120, "Max 120"),
    cabin: z.string().optional().or(z.literal("")),
  })
  .refine((v) => v.endTime > v.startTime, {
    message: "End time must be after start time",
    path: ["endTime"],
  });

type ScheduleFormValues = z.infer<typeof scheduleSchema>;

function toSeconds(t: string): string {
  return t.length === 5 ? `${t}:00` : t;
}

export function DoctorSchedulesPage() {
  const session = useOpSession();
  const unitId = session.unitId ?? 1;

  const [departmentFilter, setDepartmentFilter] = useState<number | null>(null);
  const [doctorFilter, setDoctorFilter] = useState<number | null>(null);
  const [showForm, setShowForm] = useState<boolean>(true);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const departmentsQuery = useDepartments(unitId);
  const doctorsQuery = useDoctorsMaster(unitId);
  const schedulesQuery = useDoctorScheduleList(
    departmentFilter ?? undefined,
    doctorFilter ?? undefined,
  );
  const create = useCreateSchedule();

  const doctorNameMap = useMemo(() => {
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

  const departmentNameMap = useMemo(() => {
    const map = new Map<number, string>();
    const depts = departmentsQuery.data ?? [];
    for (const d of depts) {
      if (d.id != null) {
        const name =
          d.description ||
          ((d as unknown as Record<string, unknown>).name as string) ||
          d.code ||
          `#${d.id}`;
        map.set(d.id, name);
      }
    }
    return map;
  }, [departmentsQuery.data]);

  const rows = useMemo(() => {
    return (schedulesQuery.data ?? []).map((s) => ({
      ...s,
      doctorName: doctorNameMap.get(s.doctorId) ?? `Doctor #${s.doctorId}`,
      departmentName:
        s.departmentId != null
          ? (departmentNameMap.get(s.departmentId) ?? `#${s.departmentId}`)
          : "—",
    }));
  }, [schedulesQuery.data, doctorNameMap, departmentNameMap]);

  const form = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleSchema) as any,
    defaultValues: {
      doctorId: "" as unknown as number,
      departmentId: undefined,
      dayOfWeek: "" as unknown as number,
      startTime: "",
      endTime: "",
      slotMinutes: 15,
      cabin: "",
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setSuccessMessage(null);
    setErrorMessage(null);
    try {
      await create.mutateAsync({
        doctorId: values.doctorId,
        params: {
          unitId,
          dayOfWeek: values.dayOfWeek,
          startTime: toSeconds(values.startTime),
          endTime: toSeconds(values.endTime),
          slotMinutes: values.slotMinutes,
          departmentId: values.departmentId,
          cabin: values.cabin || undefined,
        },
      });
      setSuccessMessage("Schedule created successfully.");
      form.reset({
        doctorId: "" as unknown as number,
        departmentId: undefined,
        dayOfWeek: "" as unknown as number,
        startTime: "",
        endTime: "",
        slotMinutes: 15,
        cabin: "",
      });
      await schedulesQuery.refetch();
    } catch (err) {
      setErrorMessage(
        (err as ApiError)?.message || (err as Error)?.message || "Failed to create schedule",
      );
    }
  });

  const columns: DataTableColumn<DoctorSchedule & { doctorName: string; departmentName: string }>[] = [
    {
      key: "doctor",
      header: "Doctor",
      accessor: (s) => s.doctorName,
    },
    {
      key: "department",
      header: "Department",
      accessor: (s) => s.departmentName,
    },
    {
      key: "day",
      header: "Day",
      accessor: (s) => DAY_NAMES[(s.dayOfWeek ?? 1) - 1] ?? s.dayOfWeek,
    },
    {
      key: "start",
      header: "Start",
      accessor: (s) => s.startTime,
    },
    {
      key: "end",
      header: "End",
      accessor: (s) => s.endTime,
    },
    {
      key: "slot",
      header: "Slot (min)",
      accessor: (s) => s.slotMinutes ?? 15,
    },
    {
      key: "cabin",
      header: "Cabin",
      accessor: (s) => s.cabin ?? "—",
    },
  ];

  return (
    <div className="page page--doctor-schedules">
      <header className="page-header">
        <div>
          <h1>Doctor Schedules</h1>
          <p className="muted">Manage weekly doctor schedules.</p>
        </div>
        <Button type="button" variant="primary" onClick={() => setShowForm(true)}>
          + New schedule
        </Button>
      </header>

      <section className="card">
        <div className="form-row form-row--two-col">
          <FormField label="Department">
            <HimsSelect
              value={departmentFilter ?? ""}
              onChange={(e) => setDepartmentFilter(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">All departments</option>
              {(departmentsQuery.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.description ?? d.code ?? d.id}
                </option>
              ))}
            </HimsSelect>
          </FormField>
          <FormField label="Doctor">
            <HimsSelect
              value={doctorFilter ?? ""}
              onChange={(e) => setDoctorFilter(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">All doctors</option>
              {(doctorsQuery.data ?? []).map((d) => {
                const id = Number(d.id);
                const fullName = [d.firstName, d.lastName].filter(Boolean).join(" ");
                const name = fullName
                  ? (fullName.startsWith("Dr.") ? fullName : `Dr. ${fullName}`)
                  : (d.name ? String(d.name) : `Doctor #${id}`);
                return (
                  <option key={id} value={id}>
                    {name}
                  </option>
                );
              })}
            </HimsSelect>
          </FormField>
        </div>

        {schedulesQuery.isError ? (
          <p className="alert alert--error" role="alert">
            Could not load schedules: {String((schedulesQuery.error as Error)?.message ?? schedulesQuery.error)}
          </p>
        ) : null}

        <DataTable
          rows={rows}
          columns={columns}
          rowKey={(s) => s.id}
          loading={schedulesQuery.isLoading}
          ariaLabel="Schedules"
          emptyState={
            !schedulesQuery.isError && !schedulesQuery.isLoading && rows.length === 0 ? (
              <div className="empty-state">
                <p>No schedules found.</p>
              </div>
            ) : undefined
          }
        />
      </section>

      {showForm ? (
        <section className="card" aria-label="New schedule">
          <h2>New schedule</h2>
          {successMessage ? (
            <p className="alert alert--success" role="status">
              {successMessage}
            </p>
          ) : null}
          {errorMessage ? (
            <p className="alert alert--error" role="alert">
              {errorMessage}
            </p>
          ) : null}
          {create.isError ? (
            <p className="alert alert--error" role="alert">
              {String((create.error as Error)?.message ?? create.error)}
            </p>
          ) : null}

          <form onSubmit={onSubmit}>
            <div className="form-row form-row--two-col">
              <FormField label="Doctor" required error={form.formState.errors.doctorId?.message}>
                <HimsSelect {...form.register("doctorId")}>
                  <option value="">— Select —</option>
                  {(doctorsQuery.data ?? []).map((d) => {
                    const id = Number(d.id);
                    const fullName = [d.firstName, d.lastName].filter(Boolean).join(" ");
                    const name = fullName
                      ? (fullName.startsWith("Dr.") ? fullName : `Dr. ${fullName}`)
                      : (d.name ? String(d.name) : `Doctor #${id}`);
                    return (
                      <option key={id} value={id}>
                        {name}
                      </option>
                    );
                  })}
                </HimsSelect>
              </FormField>
              <FormField label="Department" error={form.formState.errors.departmentId?.message}>
                <HimsSelect {...form.register("departmentId")}>
                  <option value="">— Select —</option>
                  {(departmentsQuery.data ?? []).map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.description ?? d.code ?? d.id}
                    </option>
                  ))}
                </HimsSelect>
              </FormField>
            </div>

            <div className="form-row form-row--two-col">
              <FormField label="Day of week" required error={form.formState.errors.dayOfWeek?.message}>
                <HimsSelect {...form.register("dayOfWeek")}>
                  <option value="">— Select —</option>
                  {DAY_NAMES.map((name, idx) => (
                    <option key={idx + 1} value={idx + 1}>
                      {name}
                    </option>
                  ))}
                </HimsSelect>
              </FormField>
              <FormField label="Slot minutes" helper="5-120 (default 15)" error={form.formState.errors.slotMinutes?.message}>
                <TextInput type="number" min={5} max={120} {...form.register("slotMinutes")} />
              </FormField>
            </div>

            <div className="form-row form-row--two-col">
              <FormField label="Start time" required error={form.formState.errors.startTime?.message}>
                <TextInput type="time" step={60} {...form.register("startTime")} />
              </FormField>
              <FormField label="End time" required error={form.formState.errors.endTime?.message}>
                <TextInput type="time" step={60} {...form.register("endTime")} />
              </FormField>
            </div>

            <div className="form-row form-row--two-col">
              <FormField label="Cabin" error={form.formState.errors.cabin?.message}>
                <TextInput {...form.register("cabin")} />
              </FormField>
            </div>

            <footer className="add-appt-form__actions">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setShowForm(false)}
                disabled={create.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" loading={create.isPending} loadingText="Creating…">
                Create schedule
              </Button>
            </footer>
          </form>
        </section>
      ) : null}
    </div>
  );
}

export { DoctorSchedulesPage as DoctorSchedulePage };
export { DoctorSchedulesPage as DoctorSchedules };
export default DoctorSchedulesPage;
