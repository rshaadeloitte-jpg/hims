import { Box, chakra, Flex, Heading, HStack, SimpleGrid, Text } from "@chakra-ui/react";
import { Link } from "react-router-dom";
import { AppShell, LayoutIcon, NAV_SECTIONS } from "../../components/layout";
import { PageHeader } from "../../components/ui";

const ModuleLink = chakra(Link);

/**
 * Landing page: a module launcher built from the same NAV_SECTIONS the
 * sidebar uses, so it can never link to a route the sidebar doesn't know.
 */
export function HomePage() {
  return (
    <AppShell>
      <PageHeader
        title="Welcome to HIMS"
        description="Jump straight to a module, or press / to search."
      />
      <SimpleGrid columns={{ base: 1, md: 2, xl: 3 }} gap={4}>
        {NAV_SECTIONS.map((section) => (
          <Box
            as="section"
            key={section.id}
            aria-labelledby={`home-${section.id}`}
            bg="surface.panel"
            borderWidth="1px"
            borderStyle="solid"
            borderColor="border.DEFAULT"
            borderRadius="lg"
            boxShadow="xs"
            p={4}
            minW={0}
          >
            <HStack gap={2.5} mb={3}>
              <Flex
                w="32px"
                h="32px"
                flex="none"
                align="center"
                justify="center"
                borderRadius="md"
                bg="deloitte.black"
                color="white"
              >
                <LayoutIcon name={section.icon} size={17} />
              </Flex>
              <Heading as="h2" id={`home-${section.id}`} size="sm" m={0} flex={1} truncate>
                {section.title}
              </Heading>
              <Text fontSize="xs" color="text.muted" m={0}>
                {section.items.length}
              </Text>
            </HStack>
            <Flex as="ul" listStyleType="none" m={0} p={0} gap={1.5} wrap="wrap">
              {section.items.map((item) => (
                <li key={item.to + item.label}>
                  <ModuleLink
                    to={item.to}
                    display="inline-flex"
                    alignItems="center"
                    minH="32px"
                    px={2.5}
                    fontSize="13px"
                    color="text.primary"
                    bg="surface.muted"
                    borderWidth="1px"
                    borderStyle="solid"
                    borderColor="border.DEFAULT"
                    borderRadius="md"
                    transition="background 150ms ease, border-color 150ms ease"
                    _hover={{ bg: "brand.50", borderColor: "brand.500", textDecoration: "none" }}
                    _focusVisible={{ outline: "2px solid {colors.border.focus}", outlineOffset: "2px" }}
                  >
                    {item.label}
                  </ModuleLink>
                </li>
              ))}
            </Flex>
          </Box>
        ))}
      </SimpleGrid>
    </AppShell>
  );
}

export default HomePage;
