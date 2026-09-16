import { Card } from "@chakra-ui/react";
import type { ReactNode } from "react";

/**
 * Brand card — white surface with an optional Deloitte-green top accent.
 * Replaces the legacy `.card` / `.deloitte-card` CSS classes.
 */
export function HimsCard({
  accent = false,
  children,
}: {
  accent?: boolean;
  children: ReactNode;
}) {
  return (
    <Card.Root
      bg="white"
      border="1px solid"
      borderColor="gray.200"
      borderRadius="lg"
      boxShadow="sm"
      borderTop={accent ? "3px solid" : undefined}
      borderTopColor={accent ? "brand.500" : undefined}
      overflow="hidden"
    >
      <Card.Body p={5}>{children}</Card.Body>
    </Card.Root>
  );
}
