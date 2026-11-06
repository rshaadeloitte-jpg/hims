import {
  Box,
  chakra,
  Flex,
  Heading,
  HStack,
  SimpleGrid,
  Skeleton,
  Text,
} from "@chakra-ui/react";
import { Link } from "react-router-dom";
import { AppShell, LayoutIcon, useCurrentUser } from "../../components/layout";
import { PageHeader, StatusBadge } from "../../components/ui";
import { useAvailableBeds } from "../adt/queries";
import { useLabOrders } from "../lab/queries";
import { useCounters, useTokens } from "../queue/queries";

const ActionLink = chakra(Link);

interface QuickActionItem {
  title: string;
  to: string;
  description: string;
  icon: string;
}

const QUICK_ACTIONS: QuickActionItem[] = [
  {
    title: "Patient Registration",
    to: "/patients/register",
    description: "Register new outpatient or inpatient patient records",
    icon: "user",
  },
  {
    title: "New Appointment",
    to: "/m/op/addAppointment",
    description: "Schedule consultations and doctor outpatient slots",
    icon: "calendar",
  },
  {
    title: "Laboratory",
    to: "/lab",
    description: "Access lab orders, worklists, and test results",
    icon: "activity",
  },
  {
    title: "Queue Dashboard",
    to: "/queue",
    description: "Monitor live waiting tokens and service counters",
    icon: "monitor",
  },
];

interface MetricItem {
  id: string;
  label: string;
  value: number | undefined;
  isLoading: boolean;
  isError: boolean;
  description: string;
  icon: string;
}

/**
 * Staff landing page: real-time hospital operational metrics, signed-in user
 * context, and high-frequency quick action workflows.
 */
export function HomePage() {
  const currentUser = useCurrentUser();

  const {
    data: queueTokens,
    isLoading: queueLoading,
    isError: queueError,
  } = useTokens();

  const {
    data: counters,
    isLoading: countersLoading,
    isError: countersError,
  } = useCounters();

  const {
    data: beds,
    isLoading: bedsLoading,
    isError: bedsError,
  } = useAvailableBeds();

  const {
    data: labOrders,
    isLoading: labLoading,
    isError: labError,
  } = useLabOrders();

  const metrics: MetricItem[] = [
    {
      id: "patients-waiting",
      label: "Patients Waiting",
      value: queueTokens
        ? queueTokens.filter((t) => t.status === 1).length
        : undefined,
      isLoading: queueLoading,
      isError: queueError,
      description: "Tokens waiting to be called today",
      icon: "monitor",
    },
    {
      id: "active-counters",
      label: "Service Counters",
      value: counters ? counters.filter((c) => c.isActive).length : undefined,
      isLoading: countersLoading,
      isError: countersError,
      description: "Active operational service counters",
      icon: "grid",
    },
    {
      id: "available-beds",
      label: "Available Beds",
      value: beds ? beds.length : undefined,
      isLoading: bedsLoading,
      isError: bedsError,
      description: "Unoccupied inpatient beds",
      icon: "bed",
    },
    {
      id: "lab-orders",
      label: "Lab Orders",
      value: labOrders ? labOrders.length : undefined,
      isLoading: labLoading,
      isError: labError,
      description: "All laboratory orders on record",
      icon: "activity",
    },
  ];

  return (
    <AppShell>
      <Box w="full" minW={0}>
        <PageHeader
          title="Staff Operations Dashboard"
          description="Live hospital overview, active queue tracking, and quick operational shortcuts."
        />

        {currentUser.authenticated && (
          <Box
            bg="surface.panel"
            borderWidth="1px"
            borderStyle="solid"
            borderColor="border.DEFAULT"
            borderRadius="lg"
            p={4}
            boxShadow="xs"
            mb={6}
            data-testid="signed-in-user-banner"
          >
            <HStack justify="space-between" align="center" wrap="wrap" gap={3}>
              <HStack gap={3}>
                <Flex
                  w="40px"
                  h="40px"
                  align="center"
                  justify="center"
                  borderRadius="full"
                  bg="deloitte.black"
                  color="white"
                  fontWeight="700"
                  fontSize="sm"
                  border="2px solid #86BC25"
                >
                  {currentUser.initials}
                </Flex>
                <Box>
                  <Text fontWeight="600" color="text.primary" fontSize="sm" m={0}>
                    {currentUser.name}
                  </Text>
                  <Text fontSize="xs" color="text.muted" m={0}>
                    {currentUser.role}
                    {currentUser.email
                      ? ` • ${currentUser.email}`
                      : currentUser.username
                        ? ` • @${currentUser.username}`
                        : ""}
                  </Text>
                </Box>
              </HStack>
              <StatusBadge tone="success">Signed In</StatusBadge>
            </HStack>
          </Box>
        )}

        <Box mb={8}>
          <Heading as="h2" size="sm" fontWeight="600" color="text.primary" mb={3}>
            Operational Metrics
          </Heading>
          <SimpleGrid columns={{ base: 1, sm: 2, lg: 4 }} gap={4}>
            {metrics.map((metric) => (
              <Box
                as="section"
                key={metric.id}
                data-testid={`metric-${metric.id}`}
                aria-labelledby={`metric-${metric.id}`}
                bg="surface.panel"
                borderWidth="1px"
                borderStyle="solid"
                borderColor="border.DEFAULT"
                borderRadius="lg"
                p={5}
                boxShadow="xs"
                minW={0}
              >
                <HStack justify="space-between" align="center" mb={3}>
                  <Heading
                    as="h3"
                    id={`metric-${metric.id}`}
                    fontSize="xs"
                    fontWeight="600"
                    textTransform="uppercase"
                    letterSpacing="0.05em"
                    color="text.muted"
                    m={0}
                    truncate
                  >
                    {metric.label}
                  </Heading>
                  <Flex
                    w="32px"
                    h="32px"
                    align="center"
                    justify="center"
                    borderRadius="md"
                    bg="deloitte.black"
                    color="#86BC25"
                    flex="none"
                  >
                    <LayoutIcon name={metric.icon} size={16} />
                  </Flex>
                </HStack>

                {metric.isLoading ? (
                  <Skeleton h="36px" w="72px" borderRadius="md" mb={2} />
                ) : metric.isError || metric.value === undefined ? (
                  <HStack align="baseline" gap={2} mb={2}>
                    <Text
                      fontSize="2xl"
                      fontWeight="700"
                      color="text.muted"
                      m={0}
                      lineHeight="1"
                    >
                      —
                    </Text>
                    <Text fontSize="xs" fontWeight="500" color="text.muted" m={0}>
                      Unavailable
                    </Text>
                  </HStack>
                ) : (
                  <Text
                    fontSize="2xl"
                    fontWeight="700"
                    color="text.primary"
                    m={0}
                    lineHeight="1"
                    mb={2}
                  >
                    {metric.value}
                  </Text>
                )}

                <Text fontSize="xs" color="text.muted" m={0} truncate>
                  {metric.description}
                </Text>
              </Box>
            ))}
          </SimpleGrid>
        </Box>

        <Box>
          <Heading as="h2" size="sm" fontWeight="600" color="text.primary" mb={3}>
            Quick Actions
          </Heading>
          <SimpleGrid columns={{ base: 1, sm: 2, lg: 4 }} gap={4}>
            {QUICK_ACTIONS.map((action) => (
              <ActionLink
                key={action.to}
                to={action.to}
                display="flex"
                flexDirection="column"
                p={5}
                bg="surface.panel"
                borderWidth="1px"
                borderStyle="solid"
                borderColor="border.DEFAULT"
                borderRadius="lg"
                boxShadow="xs"
                cursor="pointer"
                transition="border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease"
                _hover={{
                  borderColor: "#86BC25",
                  boxShadow: "md",
                  transform: "translateY(-2px)",
                  textDecoration: "none",
                }}
                _focusVisible={{
                  outline: "2px solid {colors.border.focus}",
                  outlineOffset: "2px",
                }}
              >
                <Flex
                  w="42px"
                  h="42px"
                  align="center"
                  justify="center"
                  borderRadius="md"
                  bg="deloitte.black"
                  color="#86BC25"
                  mb={3}
                >
                  <LayoutIcon name={action.icon} size={20} />
                </Flex>
                <Text
                  fontSize="md"
                  fontWeight="600"
                  color="text.primary"
                  m={0}
                  mb={1}
                >
                  {action.title}
                </Text>
                <Text fontSize="xs" color="text.muted" m={0}>
                  {action.description}
                </Text>
              </ActionLink>
            ))}
          </SimpleGrid>
        </Box>
      </Box>
    </AppShell>
  );
}

export default HomePage;
