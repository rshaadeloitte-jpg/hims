import { Badge, Box, Flex, Heading, HStack, Text, VStack } from "@chakra-ui/react";
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
      pb={4}
      mb={6}
      borderBottom="1px solid"
      borderColor="border.DEFAULT"
      minW={0}
    >
      <Flex
        direction={{ base: "column", md: "row" }}
        justify="space-between"
        align={{ base: "stretch", md: "flex-start" }}
        gap={{ base: 3, md: 4 }}
        wrap="wrap"
        minW={0}
      >
        <VStack align="flex-start" gap={1.5} minW={0} flex={{ base: "1 1 auto", md: "1 1 0" }}>
          <HStack gap={2} align="center" wrap="wrap" minW={0}>
            <Heading as="h1" size="lg" color="text.primary" m={0} fontWeight="700" wordBreak="break-word">
              {title}
            </Heading>
            {badge}
          </HStack>
          {description && (
            <Text fontSize="sm" color="text.secondary" m={0} wordBreak="break-word">
              {description}
            </Text>
          )}
        </VStack>
        {actions && (
          <HStack gap={2} wrap="wrap" w={{ base: "full", md: "auto" }} justify={{ base: "flex-start", md: "flex-end" }} flexShrink={0}>
            {actions}
          </HStack>
        )}
      </Flex>
    </Box>
  );
}
