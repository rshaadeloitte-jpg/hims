import { Badge, Box, Heading, HStack, Text, VStack } from "@chakra-ui/react";
import type { ReactNode } from "react";

export type StatusTone = "success" | "warning" | "danger" | "info" | "neutral";

const TONE_PALETTE: Record<StatusTone, string> = {
  success: "green",
  warning: "orange",
  danger: "red",
  info: "brand",
  neutral: "gray",
};

export function StatusBadge({
  tone = "neutral",
  children,
}: {
  tone?: StatusTone;
  children: ReactNode;
}) {
  return (
    <Badge colorPalette={TONE_PALETTE[tone]} variant="subtle" size="sm">
      {children}
    </Badge>
  );
}

export function PageHeader({
  title,
  description,
  badge,
  actions,
}: {
  title: string;
  description?: string;
  badge?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <Box
      pb={3}
      mb={4}
      borderBottom="1px solid"
      borderColor="gray.200"
    >
      <HStack justify="space-between" align="flex-start" gap={4} wrap="wrap">
        <VStack align="flex-start" gap={1}>
          <HStack gap={2} align="center">
            <Heading as="h1" size="lg" color="black" m={0}>
              {title}
            </Heading>
            {badge}
          </HStack>
          {description && (
            <Text fontSize="sm" color="gray.600" m={0}>
              {description}
            </Text>
          )}
        </VStack>
        {actions && (
          <HStack gap={2} wrap="wrap">
            {actions}
          </HStack>
        )}
      </HStack>
    </Box>
  );
}
