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

const SIDEBAR_BG = "#0c1116";

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
          top="62px"
          left={0}
          right={0}
          bottom={0}
          bg="blackAlpha.600"
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
        top="62px"
        left={0}
        h="calc(100vh - 62px)"
        w={{ base: "min(266px, 84vw)", lg: collapsed ? "72px" : "266px" }}
        transform={{ base: mobileOpen ? "none" : "translateX(-105%)", lg: "none" }}
        transition="width 200ms ease, transform 200ms ease"
        bg={SIDEBAR_BG}
        borderRight="1px solid"
        borderColor="gray.800"
        zIndex={50}
        display="flex"
        flexDirection="column"
        overflowY="auto"
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
                    style={{ textDecoration: "none", display: "block" }}
                  >
                    <Flex
                      justify="center"
                      p={1.5}
                      borderRadius="md"
                      color={active ? "brand.400" : "gray.400"}
                      _hover={{ bg: "whiteAlpha.100", color: "white" }}
                    >
                      <Flex
                        w="34px"
                        h="34px"
                        align="center"
                        justify="center"
                        borderRadius="md"
                        bg={active ? "rgba(134, 188, 37, 0.16)" : "whiteAlpha.100"}
                        border="1px solid"
                        borderColor={active ? "brand.500" : "whiteAlpha.100"}
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
                    color={active ? "brand.400" : "gray.400"}
                    fontSize="11px"
                    fontWeight="extrabold"
                    letterSpacing="0.09em"
                    textTransform="uppercase"
                    _hover={{ bg: "whiteAlpha.100", color: "white" }}
                    aria-expanded={open}
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
                        bg={active ? "rgba(134, 188, 37, 0.16)" : "whiteAlpha.100"}
                        border="1px solid"
                        borderColor={active ? "brand.500" : "whiteAlpha.100"}
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
                    {section.items.map((item) => (
                      <Box as="li" key={item.to + item.label}>
                        <NavLink
                          to={item.to}
                          end={item.end}
                          onClick={onCloseMobile}
                          style={{ textDecoration: "none", display: "block" }}
                        >
                          {({ isActive }) => (
                            <Flex
                              align="center"
                              gap={2.5}
                              ml="17px"
                              pl="14px"
                              pr={2.5}
                              py={2}
                              borderLeft="1px solid"
                              borderColor="whiteAlpha.200"
                              borderRadius="0 8px 8px 0"
                              position="relative"
                              fontSize="sm"
                              fontWeight={isActive ? "bold" : "medium"}
                              color={isActive ? "white" : "gray.300"}
                              bg={isActive ? "rgba(134, 188, 37, 0.15)" : "transparent"}
                              _hover={{ bg: "whiteAlpha.100", color: "white" }}
                            >
                              <Box
                                position="absolute"
                                left="-1px"
                                top="8px"
                                bottom="8px"
                                w="2px"
                                borderRadius="2px"
                                bg={isActive ? "brand.500" : "transparent"}
                                boxShadow={isActive ? "0 0 8px rgba(134,188,37,.8)" : undefined}
                                aria-hidden="true"
                              />
                              <Text truncate>{item.label}</Text>
                            </Flex>
                          )}
                        </NavLink>
                      </Box>
                    ))}
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
              <Text fontSize="11px" color="gray.400" lineHeight={1.4}>
                All modules verified · Deloitte Digital
              </Text>
            </Box>
          </Box>
        )}
      </Box>
    </>
  );
}
