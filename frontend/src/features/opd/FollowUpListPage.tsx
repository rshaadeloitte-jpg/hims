import { useState } from "react";
import { Button, HStack, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useCancelFollowUp, useCompleteFollowUp, useFollowUpList } from "./queries";
import type { FollowUpListState } from "./queries";

/**
 * M02 Follow-up List (G-1). Three view modes:
 *  - by parent appointment: GET /appointments/{id}/follow-ups
 *  - by patient + date:    GET /appointments/follow-ups/patient/{id}?date=
 *  - by doctor + date:     GET /appointments/follow-ups/doctor/{id}?date=
 */
export function FollowUpListPage() {
  const [mode, setMode] = useState<"appointment" | "patient" | "doctor">("appointment");
  const [apptId, setApptId] = useState("");
  const [patientId, setPatientId] = useState("");
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));

  const state: FollowUpListState = (() => {
    if (mode === "appointment") return { appointmentId: apptId ? Number(apptId) : undefined };
    if (mode === "patient") return { patientId: patientId ? Number(patientId) : undefined, date };
    return { doctorId: doctorId ? Number(doctorId) : undefined, date };
  })();

  const list = useFollowUpList(state);
  const cancel = useCancelFollowUp();
  const complete = useCompleteFollowUp();

  return (
    <div className="p-4 border rounded space-y-3" aria-label="follow-up-list">
      <h2 className="text-xl font-semibold">Follow-up Visits</h2>

      <HStack gap={2} align="end" wrap="wrap">
        <FormField label="Mode">
          <HimsSelect
            value={mode}
            onChange={(e) => setMode(e.target.value as typeof mode)}
          >
            <option value="appointment">By parent appointment</option>
            <option value="patient">By patient + date</option>
            <option value="doctor">By doctor + date</option>
          </HimsSelect>
        </FormField>
        {mode === "appointment" && (
          <FormField label="Appointment ID">
            <TextInput type="number" value={apptId} onChange={(e) => setApptId(e.target.value)} />
          </FormField>
        )}
        {mode === "patient" && (
          <FormField label="Patient ID">
            <TextInput type="number" value={patientId} onChange={(e) => setPatientId(e.target.value)} />
          </FormField>
        )}
        {mode === "doctor" && (
          <FormField label="Doctor ID">
            <TextInput type="number" value={doctorId} onChange={(e) => setDoctorId(e.target.value)} />
          </FormField>
        )}
        {mode !== "appointment" && (
          <FormField label="Date">
            <TextInput type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </FormField>
        )}
      </HStack>

      {list.isLoading && <p className="text-sm text-gray-500">Loading...</p>}
      {list.isError && <p className="text-sm text-red-600">{(list.error as Error).message}</p>}
      {list.data && list.data.length === 0 && <p className="text-sm text-gray-500">No follow-ups.</p>}
      {list.data && list.data.length > 0 && (
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Doctor</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {list.data.map((f) => (
              <Table.Row key={f.id}>
                <Table.Cell>{f.id}</Table.Cell>
                <Table.Cell>{f.followUpDate}</Table.Cell>
                <Table.Cell>{f.patientId}</Table.Cell>
                <Table.Cell>{f.doctorId ?? "-"}</Table.Cell>
                <Table.Cell>
                  {f.followUpStatus === 1 ? "SCHEDULED" : f.followUpStatus === 2 ? "COMPLETED" : "CANCELLED"}
                </Table.Cell>
                <Table.Cell>
                  <Button
                    type="button"
                    variant="primary"
                    size="sm"
                    disabled={complete.isPending || f.followUpStatus === 2}
                    loading={complete.isPending}
                    onClick={() => complete.mutate(f.id)}
                  >
                    Complete
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    disabled={cancel.isPending || f.followUpStatus === 3}
                    loading={cancel.isPending}
                    onClick={() => cancel.mutate(f.id)}
                  >
                    Cancel
                  </Button>
                </Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
      )}
    </div>
  );
}
