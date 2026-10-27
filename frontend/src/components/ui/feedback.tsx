import { Button, Flex, Spinner, Text, VStack } from "@chakra-ui/react";
import type { ReactNode } from "react";

export function LoadingState({ label = "Loading…", retry }: { label?: string; retry?: () => void }) {
  return (
    <Flex align="center" justify="center" py={12} gap={3} direction="column">
      <Flex align="center" justify="center" gap={3}>
        <Spinner size="md" color="brand.600" />
        <Text color="text.secondary">{label}</Text>
      </Flex>
      {retry && (
        <Button size="sm" variant="secondary" onClick={retry}>
          Retry
        </Button>
      )}
    </Flex>
  );
}

export function ErrorState({
  title = "Something went wrong",
  description,
  retry,
}: {
  title?: string;
  description?: string;
  retry?: () => void;
}) {
  return (
    <VStack py={12} gap={2} textAlign="center">
      <Text fontWeight="600" color="danger.DEFAULT" fontSize="md">
        {title}
      </Text>
      {description && (
        <Text fontSize="sm" color="text.secondary" maxW="md">
          {description}
        </Text>
      )}
      {retry && (
        <Button size="sm" variant="secondary" onClick={retry}>
          Retry
        </Button>
      )}
    </VStack>
  );
}

export function EmptyStateView({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <VStack py={12} gap={2} textAlign="center">
      <Text fontWeight="600" color="text.primary" fontSize="md">
        {title}
      </Text>
      {description && (
        <Text fontSize="sm" color="text.secondary" maxW="md">
          {description}
        </Text>
      )}
      {action}
    </VStack>
  );
}
