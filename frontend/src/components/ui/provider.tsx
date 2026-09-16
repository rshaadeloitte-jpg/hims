import { ChakraProvider } from "@chakra-ui/react";
import type { ReactNode } from "react";
import { himsSystem } from "../../theme/himsTheme";

/**
 * Root UI provider — Chakra styling engine with the HIMS Deloitte theme.
 * Mount once in main.tsx. Light theme only (enterprise default).
 */
export function Provider({ children }: { children: ReactNode }) {
  return <ChakraProvider value={himsSystem}>{children}</ChakraProvider>;
}
