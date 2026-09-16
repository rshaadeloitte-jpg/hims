import {
  Avatar,
  Badge,
  Box,
  Button,
  HStack,
  Separator,
  Text,
  VStack,
} from "@chakra-ui/react";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LayoutIcon } from "./LayoutIcon";
import { signOut, useCurrentUser } from "./useCurrentUser";

/**
 * Right-side user profile dropdown in the top header.
 * Accessible: Escape closes, outside click closes, aria-expanded/haspopup.
 */
export function UserMenu() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (e: PointerEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open ]);

  return (
    <Box position="relative" ref={rootRef}>
      <Button
        variant="ghost"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`${user.name} — profile menu`}
        onClick={() => setOpen((v) => !v)}
        h="auto"
        px={2}
        py={1.5}
        borderRadius="lg"
      >
        <HStack gap={2.5}>
          <Avatar.Root
            size="sm"
            border="2px solid"
            borderColor="brand.500"
            borderRadius="full"
          >
            <Avatar.Fallback name={user.name} fontSize="xs" fontWeight="bold" />
          </Avatar.Root>
          <VStack
            align="flex-start"
            gap={0}
            lineHeight={1.2}
            display={{ base: "none", md: "flex" }}
          >
            <Text fontSize="sm" fontWeight="bold" color="black" maxW="150px" truncate>
              {user.name}
            </Text>
            <Text fontSize="xs" color="gray.600">
              {user.role}
            </Text>
          </VStack>
          <Box
            color="gray.500"
            display={{ base: "none", md: "block" }}
            transform={open ? "rotate(180deg)" : undefined}
            transition="transform 120ms ease"
          >
            <LayoutIcon name="chevron" size={15} />
          </Box>
        </HStack>
      </Button>

      {open && (
        <Box
          role="menu"
          aria-label="User profile"
          position="absolute"
          right={0}
          top="calc(100% + 10px)"
          w="280px"
          bg="white"
          border="1px solid"
          borderColor="gray.200"
          borderRadius="xl"
          boxShadow="lg"
          overflow="hidden"
          zIndex={90}
        >
          <HStack
            gap={3}
            p={3.5}
            bg="gray.50"
            borderBottom="1px solid"
            borderColor="gray.200"
            align="flex-start"
          >
            <Avatar.Root
              size="md"
              border="2px solid"
              borderColor="brand.500"
              borderRadius="full"
            >
              <Avatar.Fallback name={user.name} fontWeight="bold" />
            </Avatar.Root>
            <VStack align="flex-start" gap={0.5} minW={0}>
              <Text fontSize="sm" fontWeight="bold" truncate maxW="full">
                {user.name}
              </Text>
              <Text fontSize="xs" color="gray.600" truncate maxW="full">
                {user.email || `@${user.username}`}
              </Text>
              <Badge colorPalette="brand" variant="subtle" size="xs" mt={1}>
                {user.role}
              </Badge>
            </VStack>
          </HStack>

          <VStack gap={0.5} p={2} align="stretch">
            <Button
              asChild
              variant="ghost"
              justifyContent="flex-start"
              fontWeight="semibold"
            >
              <Link
                to="/auth/admin"
                role="menuitem"
                onClick={() => setOpen(false)}
              >
                <HStack gap={2.5}>
                  <LayoutIcon name="user" size={16} />
                  <span>My profile &amp; roles</span>
                </HStack>
              </Link>
            </Button>
            <Button
              asChild
              variant="ghost"
              justifyContent="flex-start"
              fontWeight="semibold"
            >
              <Link
                to="/commons/config"
                role="menuitem"
                onClick={() => setOpen(false)}
              >
                <HStack gap={2.5}>
                  <LayoutIcon name="settings" size={16} />
                  <span>Settings</span>
                </HStack>
              </Link>
            </Button>
            <Separator />
            <Button
              variant="ghost"
              justifyContent="flex-start"
              fontWeight="semibold"
              color="red.700"
              _hover={{ bg: "red.50" }}
              role="menuitem"
              onClick={() => {
                setOpen(false);
                signOut((path) => navigate(path));
              }}
            >
              <HStack gap={2.5}>
                <LayoutIcon name="logout" size={16} />
                <span>Sign out</span>
              </HStack>
            </Button>
          </VStack>

          {!user.authenticated && (
            <Text
              fontSize="xs"
              color="gray.600"
              px={3.5}
              pb={3.5}
              pt={2}
              borderTop="1px dashed"
              borderColor="gray.200"
            >
              Browsing with a demo identity.{" "}
              <Link to="/auth/login" onClick={() => setOpen(false)}>
                Sign in
              </Link>{" "}
              to load your real profile.
            </Text>
          )}
        </Box>
      )}
    </Box>
  );
}
