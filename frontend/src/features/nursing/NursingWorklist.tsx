import { useState } from "react";
import { Box, Card, Heading, HStack, SimpleGrid, Table, Text, VStack } from "@chakra-ui/react";
import { useVitals, useNursingTasks } from "./queries";
import { TextInput } from "../../components/ui";

export function NursingWorklist() {
  const [patientId, setPatientId] = useState(0);
  const [admissionId, setAdmissionId] = useState(0);
  const { data: vitals = [] } = useVitals(patientId);
  const { data: tasks = [] } = useNursingTasks();

  const recentVitals = vitals.slice(-20);

  // Organize tasks by status (Linear-style Kanban)
  const taskColumns = {
    "To Do": tasks.filter((t) => !t.completed && !t.cancelled),
    "In Progress": tasks.filter((t) => t.assignedTo && !t.completed && !t.cancelled),
    "Completed": tasks.filter((t) => t.completed),
    "Cancelled": tasks.filter((t) => t.cancelled),
  };

  return (
    <Box p={4} className="space-y-6">
      <HStack justify="space-between" align="center" wrap="wrap" gap={4} mb={6}>
        <Heading as="h1" size="lg">Nursing Worklist</Heading>
        <HStack gap={4}>
          <TextInput
            type="number"
            placeholder="Patient ID"
            value={patientId || ""}
            onChange={(e) => setPatientId(Number(e.target.value))}
          />
          <TextInput
            type="number"
            placeholder="Admission ID"
            value={admissionId || ""}
            onChange={(e) => setAdmissionId(Number(e.target.value))}
          />
        </HStack>
      </HStack>

      {patientId > 0 && (
        <Card.Root mb={6}>
          <Card.Header>
            <Card.Title>Vitals Trend (Last 20)</Card.Title>
          </Card.Header>
          <Card.Body>
            <Table.Root size="sm" variant="line">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeader>Time</Table.ColumnHeader>
                  <Table.ColumnHeader>Temp (°C)</Table.ColumnHeader>
                  <Table.ColumnHeader>Pulse (bpm)</Table.ColumnHeader>
                  <Table.ColumnHeader>BP (mmHg)</Table.ColumnHeader>
                  <Table.ColumnHeader>SpO₂ (%)</Table.ColumnHeader>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {recentVitals.map((v) => (
                  <Table.Row key={v.vitalId}>
                    <Table.Cell>{new Date(v.recordedAt).toLocaleTimeString()}</Table.Cell>
                    <Table.Cell>{v.temperature ?? "—"}</Table.Cell>
                    <Table.Cell>{v.pulse ?? "—"}</Table.Cell>
                    <Table.Cell>
                      {v.bloodPressureSystolic && v.bloodPressureDiastolic
                        ? `${v.bloodPressureSystolic}/${v.bloodPressureDiastolic}`
                        : "—"}
                    </Table.Cell>
                    <Table.Cell>{v.spO2 ?? "—"}</Table.Cell>
                  </Table.Row>
                ))}
                {recentVitals.length === 0 && (
                  <Table.Row>
                    <Table.Cell colSpan={5}>No vitals recorded.</Table.Cell>
                  </Table.Row>
                )}
              </Table.Body>
            </Table.Root>
          </Card.Body>
        </Card.Root>
      )}

      <SimpleGrid columns={{ base: 1, sm: 2, lg: 4 }} gap={4}>
        {Object.entries(taskColumns).map(([status, columnTasks]) => (
          <Card.Root key={status}>
            <Card.Header bg="gray.50" py={3}>
              <Card.Title fontSize="sm" fontWeight="semibold">
                {status} ({columnTasks.length})
              </Card.Title>
            </Card.Header>
            <Card.Body maxH="400px" overflowY="auto" p={3}>
              <VStack align="stretch" gap={2}>
                {columnTasks.map((task) => (
                  <Box
                    key={task.taskId}
                    borderWidth="1px"
                    borderRadius="md"
                    p={3}
                    bg="white"
                    _hover={{ shadow: "sm" }}
                  >
                    <Text fontWeight="medium" fontSize="sm">{task.taskType}</Text>
                    <Text fontSize="xs" color="gray.600" lineClamp={2}>{task.description}</Text>
                    <HStack justify="space-between" fontSize="xs" color="gray.500" mt={2}>
                      <span>Patient: {task.patientId}</span>
                      <span>Admission: {task.admissionId}</span>
                    </HStack>
                  </Box>
                ))}
                {columnTasks.length === 0 && (
                  <Text textAlign="center" color="gray.500" py={4} fontSize="sm">No tasks</Text>
                )}
              </VStack>
            </Card.Body>
          </Card.Root>
        ))}
      </SimpleGrid>
    </Box>
  );
}