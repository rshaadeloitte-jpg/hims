import {
  Badge,
  Box,
  Button,
  Flex,
  HStack,
  IconButton,
  Input,
  InputGroup,
  Kbd,
  Separator,
  Text,
  VStack,
} from "@chakra-ui/react";
import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { DeloitteLogo } from "../DeloitteIcon";
import { LayoutIcon } from "./LayoutIcon";
import { NAV_FLAT } from "./navConfig";
import { UserMenu } from "./UserMenu";

interface TopHeaderProps {
  onToggleSidebar: () => void;
  sidebarCollapsed: boolean;
}

function useActiveSectionTitle(): string {
  const { pathname } = useLocation();
  return useMemo(() => {
    const exact = NAV_FLAT.find((i) => i.end && pathname === i.to);
    if (exact) return `${exact.section} / ${exact.label}`;
    const prefix = NAV_FLAT.filter((i) => !i.end && pathname.startsWith(i.to)).sort(
      (a, b) => b.to.length - a.to.length,
    )[0];
    if (prefix) return `${prefix.section} / ${prefix.label}`;
    return "Dashboard";
  }, [pathname]);
}

/**
 * Enterprise top header: Deloitte brand (left), global search + breadcrumb
 * (center), environment badge + notifications + user profile (right).
 */
export function TopHeader({ onToggleSidebar, sidebarCollapsed }: TopHeaderProps) {
  const navigate = useNavigate();
  const breadcrumb = useActiveSectionTitle();
  const [query, setQuery] = useState("");
  const [searchFocus, setSearchFocus] = useState(false);

  const suggestions = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (q.length < 2) return [];
    return NAV_FLAT.filter(
      (i) =>
        i.label.toLowerCase().includes(q) || i.section.toLowerCase().includes(q),
    ).slice(0, 7);
  }, [query]);

  return (
    <Box
      as="header"
      position="sticky"
      top={0}
      zIndex="sticky"
      h="64px"
      bg="surface.header"
      borderBottom="2px solid"
      borderColor="brand.500"
      boxShadow="sm"
    >
      <Flex align="center" gap={4} px={4} h="full">
        <HStack gap={3} flex="1 1 auto" minW={0}>
          <IconButton
            variant="ghost"
            aria-label={sidebarCollapsed ? "Expand sidebar" : "Collapse sidebar"}
            title={sidebarCollapsed ? "Expand sidebar" : "Collapse sidebar"}
            onClick={onToggleSidebar}
            color="gray.700"
            _hover={{ bg: "gray.100", color: "gray.900" }}
          >
            <LayoutIcon name="menu" size={20} />
          </IconButton>

          <Link
            to="/masters"
            aria-label="Deloitte HIMS home"
            style={{ textDecoration: "none" }}
          >
            <HStack gap={2.5}>
              <DeloitteLogo size={30} />
              <VStack gap={0} lineHeight={1.1} align="flex-start">
                <Text fontSize="19px" fontWeight="black" color="gray.900" m={0}>
                  Deloitte<Text as="span" color="brand.500">.</Text>
                </Text>
                <Text
                  fontSize="10.5px"
                  fontWeight="bold"
                  letterSpacing="0.12em"
                  textTransform="uppercase"
                  color="gray.500"
                  m={0}
                  display={{ base: "none", sm: "block" }}
                >
                  HIMS Enterprise
                </Text>
              </VStack>
            </HStack>
          </Link>

          <Text
            ml={2}
            pl={4}
            borderLeft="1px solid"
            borderColor="gray.200"
            fontSize="sm"
            fontWeight="semibold"
            color="gray.600"
            truncate
            maxW="320px"
            display={{ base: "none", xl: "block" }}
            aria-live="polite"
          >
            {breadcrumb}
          </Text>
        </HStack>

        <Box
          className="shell-search"
          flex="0 1 440px"
          justifyContent="center"
          display={{ base: "none", md: "flex" }}
          position="relative"
          w="full"
          maxW="440px"
        >
          <InputGroup
            w="full"
            startElement={
              <Box color="gray.400" display="flex" alignItems="center">
                <LayoutIcon name="search" size={16} />
              </Box>
            }
            endElement={
              <Kbd fontSize="10px" px={1.5} py={0.5} bg="gray.100" color="gray.500" borderColor="gray.200">
                /
              </Kbd>
            }
          >
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onFocus={() => setSearchFocus(true)}
              onBlur={() => window.setTimeout(() => setSearchFocus(false), 120)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  if (suggestions.length > 0) {
                    navigate(suggestions[0].to);
                    setQuery("");
                  } else if (query.trim()) {
                    navigate("/patients");
                    setQuery("");
                  }
                }
                if (e.key === "Escape") setQuery("");
              }}
              placeholder="Search modules, bills, patients…"
              aria-label="Global search"
              h="38px"
              bg={searchFocus ? "white" : "gray.50"}
              border="1px solid"
              borderColor={searchFocus ? "brand.500" : "gray.200"}
              color="gray.800"
              borderRadius="full"
              fontSize="sm"
              boxShadow={searchFocus ? "0 0 0 3px rgba(134, 188, 37, 0.2)" : "none"}
              transition="all 0.15s ease"
            />
          </InputGroup>
          {suggestions.length > 0 && (
            <Box
              role="listbox"
              aria-label="Module suggestions"
              position="absolute"
              top="calc(100% + 8px)"
              left={0}
              right={0}
              bg="white"
              border="1px solid"
              borderColor="gray.200"
              borderRadius="xl"
              boxShadow="lg"
              overflow="hidden"
              zIndex={80}
            >
              {suggestions.map((s) => (
                <Button
                  key={`${s.section}:${s.to}`}
                  type="button"
                  role="option"
                  aria-selected="false"
                  variant="ghost"
                  w="full"
                  h="auto"
                  px={3.5}
                  py={2.5}
                  borderRadius={0}
                  justifyContent="space-between"
                  fontWeight="normal"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => {
                    navigate(s.to);
                    setQuery("");
                  }}
                >
                  <Text fontWeight="semibold">{s.label}</Text>
                  <Text
                    fontSize="11px"
                    fontWeight="bold"
                    textTransform="uppercase"
                    letterSpacing="0.06em"
                    color="gray.500"
                  >
                    {s.section}
                  </Text>
                </Button>
              ))}
            </Box>
          )}
        </Box>

        <HStack gap={2.5} flex="1 1 auto" justify="flex-end">
          <Badge
            variant="subtle"
            size="sm"
            borderRadius="full"
            px={2.5}
            py={0.5}
            bg="green.50"
            border="1px solid"
            borderColor="green.200"
            color="green.700"
            display={{ base: "none", md: "inline-flex" }}
          >
            <HStack gap={1.5}>
              <Box w="6px" h="6px" borderRadius="full" bg="green.600" />
              <Text fontSize="11px" fontWeight="bold" letterSpacing="0.05em">LIVE</Text>
            </HStack>
          </Badge>
          <Box position="relative">
            <IconButton
              variant="ghost"
              aria-label="Notifications"
              color="gray.700"
              _hover={{ bg: "gray.100", color: "gray.900" }}
            >
              <LayoutIcon name="bell" size={19} />
            </IconButton>
            <Box
              position="absolute"
              top="8px"
              right="9px"
              w="8px"
              h="8px"
              borderRadius="full"
              bg="red.600"
              border="2px solid white"
              aria-hidden="true"
            />
          </Box>
          <Separator
            orientation="vertical"
            height="7"
            borderColor="gray.200"
            display={{ base: "none", sm: "block" }}
          />
          <UserMenu />
        </HStack>
      </Flex>
    </Box>
  );
}
