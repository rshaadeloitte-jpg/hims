import { Flex, Spinner, Text, VStack } from "@chakra-ui/react";
import type { ReactNode } from "react";

export function LoadingState({ label = "Loading…" }: { label?: string }) {
  return (
    <Flex align="center" justify="center" py={12} gap={3}>
      <Spinner size="md" color="brand.600" />
      <Text color="text.secondary">{label}</Text>
    </Flex>
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
