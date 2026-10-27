import { Box, Button, Flex, HStack, Text, VStack } from "@chakra-ui/react";
import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { LayoutIcon } from "./LayoutIcon";
import { NAV_SECTIONS } from "./navConfig";

interface SidebarProps {
  collapsed: boolean;
  mobileOpen: boolean;
  onCloseMobile: () => void;
}

function sectionHasActivePath(sectionId: string, pathname: string): boolean {
  const section = NAV_SECTIONS.find((s) => s.id === sectionId);
  if (!section) return false;
  return section.items.some((item) =>
    item.end ? pathname === item.to : pathname.startsWith(item.to),
  );
}

const SIDEBAR_BG = "#1F1F1F";

/**
 * Enterprise left sidebar: grouped, collapsible sections with
 * active-route highlighting. Icon rail on desktop when collapsed,
 * overlay drawer on mobile.
 */
export function Sidebar({ collapsed, mobileOpen, onCloseMobile }: SidebarProps) {
  const { pathname } = useLocation();
  const [openSections, setOpenSections] = useState<Record<string, boolean>>(() => {
    const initial: Record<string, boolean> = {};
    for (const s of NAV_SECTIONS) {
      initial[s.id] = sectionHasActivePath(s.id, window.location.pathname);
    }
    initial["overview"] = true;
    initial["front-office"] = true;
    return initial;
  });

  useEffect(() => {
    for (const s of NAV_SECTIONS) {
      if (sectionHasActivePath(s.id, pathname)) {
        setOpenSections((prev) => (prev[s.id] ? prev : { ...prev, [s.id]: true }));
      }
    }
  }, [pathname]);

  useEffect(() => {
    document.body.style.overflow = mobileOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileOpen]);

  const toggle = (id: string) =>
    setOpenSections((prev) => ({ ...prev, [id]: !prev[id] }));

  return (
    <>
      {mobileOpen && (
        <Box
          position="fixed"
          top="64px"
          left={0}
          right={0}
          bottom={0}
          bg="blackAlpha.700"
          zIndex={40}
          onClick={onCloseMobile}
          aria-hidden="true"
          display={{ lg: "none" }}
        />
      )}
      <Box
        as="aside"
        aria-label="Primary"
        data-collapsed={collapsed || undefined}
        position={{ base: "fixed", lg: "sticky" }}
        top="64px"
        left={0}
        minH="calc(100vh - 64px)"
        w={{ base: "min(266px, 84vw)", lg: collapsed ? "72px" : "266px" }}
        transform={{ base: mobileOpen ? "none" : "translateX(-105%)", lg: "none" }}
        transition="width 200ms ease, transform 200ms ease"
        bg={SIDEBAR_BG}
        borderRight="1px solid"
        borderColor="whiteAlpha.100"
        zIndex={50}
        display="flex"
        flexDirection="column"
        overflowY="auto"
        overflowX="hidden"
        css={{
          scrollbarWidth: "thin",
          scrollbarColor: "rgba(255, 255, 255, 0.15) transparent",
        }}
      >
        <VStack as="nav" p={2.5} gap={1} flex={1} align="stretch">
          {NAV_SECTIONS.map((section) => {
            const open = collapsed ? true : (openSections[section.id] ?? false);
            const active = sectionHasActivePath(section.id, pathname);
            return (
              <Box key={section.id} borderRadius="md">
                {collapsed ? (
                  <NavLink
                    to={section.items[0].to}
                    onClick={onCloseMobile}
                    title={section.title}
                    data-active={active || undefined}
                    style={{ textDecoration: "none", display: "block" }}
                  >
                    <Flex
                      justify="center"
                      p={1.5}
                      borderRadius="md"
                      color={active ? "#FFFFFF" : "#D1D5DB"}
                      _hover={{ bg: "rgba(255, 255, 255, 0.08)", color: "#FFFFFF" }}
                      _focusVisible={{ outline: "2px solid #86BC25", outlineOffset: "2px" }}
                      data-active={active || undefined}
                    >
                      <Flex
                        w="34px"
                        h="34px"
                        align="center"
                        justify="center"
                        borderRadius="md"
                        bg={active ? "#86BC25" : "rgba(255, 255, 255, 0.08)"}
                        border="1px solid"
                        borderColor={active ? "#9CCD46" : "rgba(255, 255, 255, 0.12)"}
                        color={active ? "#1F1F1F" : "#D1D5DB"}
                      >
                        <LayoutIcon name={section.icon} size={18} />
                      </Flex>
                    </Flex>
                  </NavLink>
                ) : (
                  <Button
                    variant="ghost"
                    w="full"
                    justifyContent="flex-start"
                    px={2.5}
                    py={2}
                    h="auto"
                    color={active ? "#FFFFFF" : "#D1D5DB"}
                    fontSize="11px"
                    fontWeight="extrabold"
                    letterSpacing="0.09em"
                    textTransform="uppercase"
                    bg={active ? "rgba(134, 188, 37, 0.14)" : "transparent"}
                    _hover={{ bg: active ? "rgba(134, 188, 37, 0.22)" : "rgba(255, 255, 255, 0.08)", color: "#FFFFFF" }}
                    _focusVisible={{ outline: "2px solid #86BC25", outlineOffset: "2px" }}
                    _expanded={{ bg: "color-palette.subtle", color: "#1F1F1F" }}
                    aria-expanded={open}
                    aria-current={active ? "true" : undefined}
                    data-active={active || undefined}
                    data-expanded={open || undefined}
                    onClick={() => toggle(section.id)}
                  >
                    <HStack gap={2.5} w="full">
                      <Flex
                        w="34px"
                        h="34px"
                        align="center"
                        justify="center"
                        borderRadius="md"
                        flex="none"
                        bg={active ? "#86BC25" : "rgba(255, 255, 255, 0.08)"}
                        border="1px solid"
                        borderColor={active ? "#9CCD46" : "rgba(255, 255, 255, 0.12)"}
                        color={active ? "#1F1F1F" : "#D1D5DB"}
                      >
                        <LayoutIcon name={section.icon} size={18} />
                      </Flex>
                      <Text flex={1} textAlign="left" truncate>
                        {section.title}
                      </Text>
                      <Box
                        transform={open ? "rotate(180deg)" : undefined}
                        transition="transform 120ms ease"
                      >
                        <LayoutIcon name="chevron" size={14} />
                      </Box>
                    </HStack>
                  </Button>
                )}

                {open && !collapsed && (
                  <VStack as="ul" m={0} p={0} mt={0.5} mb={1.5} gap="1px" align="stretch" listStyleType="none">
                    {section.items.map((item) => {
                      const itemActive = item.end ? pathname === item.to : pathname.startsWith(item.to);
                      return (
                      <Box as="li" key={item.to + item.label}>
                        <NavLink
                          to={item.to}
                          end={item.end}
                          onClick={onCloseMobile}
                          data-active={itemActive || undefined}
                          style={{ textDecoration: "none", display: "block" }}
                        >
                          {({ isActive }) => (
                            <Flex
                              align="center"
                              gap={2}
                              ml="16px"
                              pl="14px"
                              pr={2.5}
                              py={1.5}
                              borderLeft="3px solid"
                              borderColor={isActive ? "brand.500" : "whiteAlpha.200"}
                              borderRadius="0 6px 6px 0"
                              position="relative"
                              fontSize="13px"
                              fontWeight={isActive ? "700" : "400"}
                              color={isActive ? "#FFFFFF" : "#D1D5DB"}
                              bg={isActive ? "rgba(134, 188, 37, 0.24)" : "transparent"}
                              borderLeftColor={isActive ? "#86BC25" : "rgba(255, 255, 255, 0.16)"}
                              _hover={{
                                bg: isActive ? "rgba(134, 188, 37, 0.36)" : "rgba(255, 255, 255, 0.08)",
                                color: "#FFFFFF",
                              }}
                              _focusVisible={{ outline: "2px solid #86BC25", outlineOffset: "2px" }}
                              data-active={isActive || undefined}
                              transition="all 0.15s ease"
                            >
                              <Text truncate>{item.label}</Text>
                              </Flex>
                          )}
                        </NavLink>
                      </Box>
                      );
                    })}
                  </VStack>
                )}
              </Box>
            );
          })}
        </VStack>

        {!collapsed && (
          <Box p={3}>
            <Box
              bg="rgba(134, 188, 37, 0.1)"
              border="1px solid"
              borderColor="rgba(134, 188, 37, 0.25)"
              borderRadius="xl"
              p={3}
            >
              <Text fontSize="xs" fontWeight="bold" color="white">
                HIMS Modernization
              </Text>
              <Text fontSize="11px" color="gray.300" lineHeight={1.4}>
                All modules verified · Deloitte Digital
              </Text>
            </Box>
          </Box>
        )}
      </Box>
    </>
  );
}
