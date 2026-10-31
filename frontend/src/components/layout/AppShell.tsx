import { Box, Flex, Text } from "@chakra-ui/react";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Sidebar } from "./Sidebar";
import { TopHeader } from "./TopHeader";

interface AppShellProps {
  children: ReactNode;
}

const COLLAPSE_KEY = "hims:shell:collapsed";

/**
 * Enterprise app shell: white top header (Deloitte brand + search +
 * user profile dropdown) with a dark grouped sidebar on the left.
 * - Desktop: sidebar collapses to an icon rail (persisted).
 * - Mobile (<1024px): sidebar becomes an overlay drawer.
 */
export function AppShell({ children }: AppShellProps) {
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    try {
      return localStorage.getItem(COLLAPSE_KEY) === "1";
    } catch {
      return false;
    }
  });
  const [mobileOpen, setMobileOpen] = useState(false);

  const toggleSidebar = useCallback(() => {
    if (window.innerWidth < 1024) {
      setMobileOpen((v) => !v);
      return;
    }
    setCollapsed((v) => {
      const next = !v;
      try {
        localStorage.setItem(COLLAPSE_KEY, next ? "1" : "0");
      } catch {
        /* ignore */
      }
      return next;
    });
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const typing =
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable);
      if (e.key === "/" && !typing) {
        e.preventDefault();
        document
          .querySelector<HTMLInputElement>(".shell-search input")
          ?.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);

  useEffect(() => {
    const onResize = () => {
      if (window.innerWidth >= 1024) setMobileOpen(false);
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  return (
    <Box
      data-testid="app-shell"
      data-collapsed={collapsed || undefined}
      minH="100dvh"
      display="flex"
      flexDirection="column"
      bg="canvas"
    >
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <TopHeader onToggleSidebar={toggleSidebar} sidebarCollapsed={collapsed} />
      <Flex flex={1} minH={0}>
        <Sidebar
          collapsed={collapsed}
          mobileOpen={mobileOpen}
          onCloseMobile={() => setMobileOpen(false)}
        />
        <Flex flex={1} minW={0} direction="column">
          <Box
            as="main"
            id="main-content"
            tabIndex={-1}
            outline="none"
            flex={1}
            w="full"
            maxW="1400px"
            mx="auto"
            px={{ base: 3.5, md: 6 }}
            py={{ base: 4, md: 6 }}
            pb={{ base: 7, md: 8 }}
            minW={0}
          >
            {children}
          </Box>
          <Flex
            as="footer"
            borderTop="1px solid"
            borderColor="border.DEFAULT"
            bg="surface.muted"
            color="text.muted"
            fontSize="xs"
            textAlign="center"
            p={3}
            align="center"
            justify="center"
            gap={2}
          >
            <Box
              w="6px"
              h="6px"
              bg="brand.500"
              borderRadius="full"
              flex="none"
              aria-hidden="true"
            />
            <Text m={0}>
              Deloitte Digital &amp; Healthcare Solutions · HIMS Modernization
              Platform · All Modules Verified
            </Text>
          </Flex>
        </Flex>
      </Flex>
    </Box>
  );
}
