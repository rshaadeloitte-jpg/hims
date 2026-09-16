import { useState } from "react";
import { Button, HStack, SimpleGrid, Table, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  useComplaints,
  useRaiseComplaint,
  useComplaint,
  useAddResolution,
  EquipmentComplaint,
  EquipmentComplaintResolution,
} from "./queries";
import {
  equipmentComplaintSchema,
  complaintResolutionWithDetailsSchema,
  type EquipmentComplaintForm,
  type ComplaintResolutionWithDetailsForm,
} from "./zodSchema";

const COMPLAINT_STATUS_LABELS: Record<number, string> = {
  1: "Open",
  2: "In Progress",
  3: "Resolved",
  4: "Closed",
};

const STATUS_BADGE: Record<number, string> = {
  1: "badge-danger",
  2: "badge-warning",
  3: "badge-success",
  4: "badge-muted",
};

const KANBAN_COLUMNS = [
  { status: 1, label: "Open", color: "kanban-open" },
  { status: 2, label: "In Progress", color: "kanban-progress" },
  { status: 3, label: "Resolved", color: "kanban-resolved" },
  { status: 4, label: "Closed", color: "kanban-closed" },
];

const emptyComplaintForm: EquipmentComplaintForm = {
  equipmentId: 0,
  categoryId: undefined,
  complaintTypeId: undefined,
  severityId: undefined,
  description: "",
  reportedBy: undefined,
  reportedDate: undefined,
  roomNo: undefined,
  extensionNo: undefined,
  assignedTo: undefined,
  expectedResolutionDate: undefined,
};

const emptyResolutionForm: ComplaintResolutionWithDetailsForm = {
  resolutionNotes: "",
  partsReplaced: undefined,
  serviceAgentId: undefined,
  assignedTo: undefined,
  assignedById: undefined,
  resolvedStatus: 3,
  resolutionDate: undefined,
  details: undefined,
};

export function ComplaintTracker() {
  const { data: complaints = [], isLoading, error } = useComplaints();
  const raise = useRaiseComplaint();
  const addResolution = useAddResolution();

  const [showForm, setShowForm] = useState(false);
  const [showKanban, setShowKanban] = useState(false);
  const [complaintForm, setComplaintForm] = useState<EquipmentComplaintForm>(emptyComplaintForm);
  const [complaintErrors, setComplaintErrors] = useState<Record<string, string>>({});

  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [resolutionForm, setResolutionForm] =
    useState<ComplaintResolutionWithDetailsForm>(emptyResolutionForm);
  const [resolutionErrors, setResolutionErrors] = useState<Record<string, string>>({});

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const { data: complaintDetail } = useComplaint(expandedId ?? 0);

  const handleRaise = () => {
    const result = equipmentComplaintSchema.safeParse(complaintForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setComplaintErrors(fieldErrors);
      return;
    }
    raise.mutate(result.data as Record<string, unknown>, {
      onSuccess: () => {
        setShowForm(false);
        setComplaintForm(emptyComplaintForm);
        setComplaintErrors({});
      },
    });
  };

  const handleResolve = () => {
    const result = complaintResolutionWithDetailsSchema.safeParse(resolutionForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setResolutionErrors(fieldErrors);
      return;
    }
    if (resolvingId) {
      addResolution.mutate(
        { complaintId: resolvingId, payload: result.data as Record<string, unknown> },
        {
          onSuccess: () => {
            setResolvingId(null);
            setResolutionForm(emptyResolutionForm);
            setResolutionErrors({});
          },
        },
      );
    }
  };

  const complaintsByStatus = (status: number) =>
    complaints.filter((c) => c.complaintStatus === status);

  if (isLoading) return <p className="muted">Loading complaints…</p>;
  if (error) return <p className="error">Error loading complaints: {error.message}</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Complaint Tracker</h1>
        <HStack gap={2} className="header-actions">
          <Button
            variant={showKanban ? "secondary" : "ghost"}
            onClick={() => setShowKanban(!showKanban)}
          >
            {showKanban ? "Table View" : "Kanban View"}
          </Button>
          <Button
            variant={showForm ? "secondary" : "primary"}
            onClick={() => {
              setShowForm(!showForm);
              setResolvingId(null);
            }}
          >
            {showForm ? "Cancel" : "Raise Complaint"}
          </Button>
        </HStack>
      </header>

      {showForm && (
        <div className="card">
          <h2>Raise New Complaint</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Equipment ID" error={complaintErrors.equipmentId} required>
              <TextInput
                id="cmp-equipmentId"
                aria-label="Equipment ID"
                type="number"
                value={complaintForm.equipmentId || ""}
                onChange={(e) =>
                  setComplaintForm({ ...complaintForm, equipmentId: Number(e.target.value) })
                }
              />
            </FormField>

            <FormField label="Category ID">
              <TextInput
                id="cmp-categoryId"
                aria-label="Category ID"
                type="number"
                value={complaintForm.categoryId ?? ""}
                onChange={(e) =>
                  setComplaintForm({
                    ...complaintForm,
                    categoryId: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Complaint Type ID">
              <TextInput
                id="cmp-complaintTypeId"
                aria-label="Complaint Type ID"
                type="number"
                value={complaintForm.complaintTypeId ?? ""}
                onChange={(e) =>
                  setComplaintForm({
                    ...complaintForm,
                    complaintTypeId: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Severity ID">
              <TextInput
                id="cmp-severityId"
                aria-label="Severity ID"
                type="number"
                value={complaintForm.severityId ?? ""}
                onChange={(e) =>
                  setComplaintForm({
                    ...complaintForm,
                    severityId: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Description" error={complaintErrors.description} required>
              <Textarea
                id="cmp-description"
                aria-label="Description"
                value={complaintForm.description}
                onChange={(e) => setComplaintForm({ ...complaintForm, description: e.target.value })}
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Reported By (User ID)">
              <TextInput
                id="cmp-reportedBy"
                aria-label="Reported By (User ID)"
                type="number"
                value={complaintForm.reportedBy ?? ""}
                onChange={(e) =>
                  setComplaintForm({
                    ...complaintForm,
                    reportedBy: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Room No.">
              <TextInput
                id="cmp-roomNo"
                aria-label="Room No."
                value={complaintForm.roomNo ?? ""}
                onChange={(e) => setComplaintForm({ ...complaintForm, roomNo: e.target.value })}
              />
            </FormField>

            <FormField label="Extension No.">
              <TextInput
                id="cmp-extensionNo"
                aria-label="Extension No."
                value={complaintForm.extensionNo ?? ""}
                onChange={(e) => setComplaintForm({ ...complaintForm, extensionNo: e.target.value })}
              />
            </FormField>

            <FormField label="Assigned To (User ID)">
              <TextInput
                id="cmp-assignedTo"
                aria-label="Assigned To (User ID)"
                type="number"
                value={complaintForm.assignedTo ?? ""}
                onChange={(e) =>
                  setComplaintForm({
                    ...complaintForm,
                    assignedTo: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Expected Resolution Date">
              <TextInput
                id="cmp-expectedResolutionDate"
                aria-label="Expected Resolution Date"
                type="date"
                value={complaintForm.expectedResolutionDate ?? ""}
                onChange={(e) =>
                  setComplaintForm({
                    ...complaintForm,
                    expectedResolutionDate: e.target.value,
                  })
                }
              />
            </FormField>
          </SimpleGrid>
          <Button variant="primary" onClick={handleRaise} disabled={raise.isPending} loading={raise.isPending} loadingText="Raising…">
            Submit Complaint
          </Button>
        </div>
      )}

      {resolvingId && (
        <div className="card">
          <h2>Resolve Complaint #{resolvingId}</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Resolution Notes" error={resolutionErrors.resolutionNotes} required>
              <Textarea
                id="res-resolutionNotes"
                aria-label="Resolution Notes"
                value={resolutionForm.resolutionNotes}
                onChange={(e) =>
                  setResolutionForm({ ...resolutionForm, resolutionNotes: e.target.value })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Parts Replaced">
              <Textarea
                id="res-partsReplaced"
                aria-label="Parts Replaced"
                value={resolutionForm.partsReplaced ?? ""}
                onChange={(e) =>
                  setResolutionForm({ ...resolutionForm, partsReplaced: e.target.value })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Service Agent ID">
              <TextInput
                id="res-serviceAgentId"
                aria-label="Service Agent ID"
                type="number"
                value={resolutionForm.serviceAgentId ?? ""}
                onChange={(e) =>
                  setResolutionForm({
                    ...resolutionForm,
                    serviceAgentId: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Assigned To">
              <TextInput
                id="res-assignedTo"
                aria-label="Assigned To"
                type="number"
                value={resolutionForm.assignedTo ?? ""}
                onChange={(e) =>
                  setResolutionForm({
                    ...resolutionForm,
                    assignedTo: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Resolved Status">
              <HimsSelect
                id="res-resolvedStatus"
                aria-label="Resolved Status"
                value={resolutionForm.resolvedStatus}
                onChange={(e) =>
                  setResolutionForm({ ...resolutionForm, resolvedStatus: Number(e.target.value) })
                }
              >
                <option value={1}>Open</option>
                <option value={2}>In Progress</option>
                <option value={3}>Resolved</option>
                <option value={4}>Closed</option>
              </HimsSelect>
            </FormField>

            <FormField label="Resolution Date">
              <TextInput
                id="res-resolutionDate"
                aria-label="Resolution Date"
                type="date"
                value={resolutionForm.resolutionDate ?? ""}
                onChange={(e) =>
                  setResolutionForm({ ...resolutionForm, resolutionDate: e.target.value })
                }
              />
            </FormField>
          </SimpleGrid>
          <Button
            variant="primary"
            onClick={handleResolve}
            disabled={addResolution.isPending}
            loading={addResolution.isPending}
            loadingText="Saving…"
          >
            Record Resolution
          </Button>
        </div>
      )}

      {showKanban ? (
        <div className="kanban-board">
          {KANBAN_COLUMNS.map((col) => (
            <div key={col.status} className={`kanban-column ${col.color}`}>
              <div className="kanban-column-header">
                <span>{col.label}</span>
                <span className="kanban-count">{complaintsByStatus(col.status).length}</span>
              </div>
              <div className="kanban-cards">
                {complaintsByStatus(col.status).map((c) => (
                  <ComplaintCard
                    key={c.id}
                    complaint={c}
                    onExpand={() => setExpandedId(expandedId === c.id ? null : c.id)}
                    onResolve={() => setResolvingId(c.id)}
                    expanded={expandedId === c.id}
                    detail={expandedId === c.id ? complaintDetail : undefined}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card">
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Complaint No.</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Equipment</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Description</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reported</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {complaints.map((c) => (
                <Table.Row key={c.id}>
                  <Table.Cell>{c.id}</Table.Cell>
                  <Table.Cell>{c.complaintNo ?? "—"}</Table.Cell>
                  <Table.Cell>{c.equipmentId}</Table.Cell>
                  <Table.Cell>{c.description}</Table.Cell>
                  <Table.Cell>
                    <span
                      className={`badge ${STATUS_BADGE[c.complaintStatus] ?? "badge-muted"}`}
                    >
                      {COMPLAINT_STATUS_LABELS[c.complaintStatus] ?? c.complaintStatus}
                    </span>
                  </Table.Cell>
                  <Table.Cell>{c.reportedDate ?? "—"}</Table.Cell>
                  <Table.Cell>
                    <HStack gap={1}>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setExpandedId(expandedId === c.id ? null : c.id)}
                      >
                        {expandedId === c.id ? "Hide" : "Detail"}
                      </Button>
                      {c.complaintStatus < 3 && (
                        <Button variant="primary" size="sm" onClick={() => setResolvingId(c.id)}>
                          Resolve
                        </Button>
                      )}
                    </HStack>
                  </Table.Cell>
                </Table.Row>
              ))}
              {complaints.length === 0 && (
                <Table.Row>
                  <Table.Cell colSpan={7}>
                    No complaints.
                  </Table.Cell>
                </Table.Row>
              )}
            </Table.Body>
          </Table.Root>
        </div>
      )}

      {expandedId && !showKanban && complaintDetail && (
        <div className="card">
          <h2>Complaint #{expandedId} Details</h2>
          <p>
            <strong>Description:</strong> {complaintDetail.complaint.description}
          </p>
          <p>
            <strong>Accession:</strong> {complaintDetail.complaint.accessionNo ?? "—"}
          </p>
          {complaintDetail.resolutions.length > 0 && (
            <>
              <h3>Resolutions</h3>
              <Table.Root size="sm" variant="line">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Notes</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Parts Replaced</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {complaintDetail.resolutions.map((r) => (
                    <Table.Row key={r.id}>
                      <Table.Cell>{r.resolutionDate ?? "—"}</Table.Cell>
                      <Table.Cell>{r.resolutionNotes}</Table.Cell>
                      <Table.Cell>{r.partsReplaced ?? "—"}</Table.Cell>
                      <Table.Cell>
                        <span className="badge badge-success">
                          {COMPLAINT_STATUS_LABELS[r.resolvedStatus] ?? r.resolvedStatus}
                        </span>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            </>
          )}
        </div>
      )}
    </section>
  );
}

function ComplaintCard({
  complaint,
  onExpand,
  onResolve,
  expanded,
  detail,
}: {
  complaint: EquipmentComplaint;
  onExpand: () => void;
  onResolve: () => void;
  expanded: boolean;
  detail?: { resolutions: EquipmentComplaintResolution[] };
}) {
  return (
    <div className="kanban-card">
      <div className="kanban-card-header">
        <span className="kanban-card-id">#{complaint.id}</span>
        {complaint.complaintNo && (
          <span className="kanban-card-no">{complaint.complaintNo}</span>
        )}
      </div>
      <div className="kanban-card-body">
        <p className="kanban-card-desc">{complaint.description}</p>
        <p className="kanban-card-meta">Eq: {complaint.equipmentId}</p>
        {complaint.accessionNo && (
          <p className="kanban-card-meta">Acc: {complaint.accessionNo}</p>
        )}
      </div>
      <HStack gap={1} className="kanban-card-actions">
        <Button variant="secondary" size="sm" onClick={onExpand}>
          {expanded ? "Hide" : "View"}
        </Button>
        {complaint.complaintStatus < 3 && (
          <Button variant="primary" size="sm" onClick={onResolve}>
            Resolve
          </Button>
        )}
      </HStack>
      {expanded && detail && detail.resolutions.length > 0 && (
        <div className="kanban-card-detail">
          <small>
            {detail.resolutions.length} resolution(s) recorded
          </small>
        </div>
      )}
    </div>
  );
}
