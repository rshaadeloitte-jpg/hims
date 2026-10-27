import { Box, Flex, Heading, SimpleGrid, Text, VStack } from "@chakra-ui/react";
import { DeloitteLogo } from "../../components/DeloitteIcon";

export function HomePage() {
  return (
    <Flex minH="100vh" align="center" justify="center" py={10}>
      <VStack gap={6} textAlign="center">
        <DeloitteLogo size={48} />
        <VStack gap={1}>
          <Heading as="h1" size="xl" color="black">
            Home Page
          </Heading>
          <Text fontSize="md" color="gray.600">
            Welcome to HIMS
          </Text>
        </VStack>
        <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} w="full" maxW="md">
          <Box p={4} bg="white" borderRadius="lg" boxShadow="sm">
            <Text fontWeight="semibold">Quick Access</Text>
            <Text fontSize="sm" color="gray.600">
              Use the sidebar to navigate modules.
            </Text>
          </Box>
          <Box p={4} bg="white" borderRadius="lg" boxShadow="sm">
            <Text fontWeight="semibold">Status</Text>
            <Text fontSize="sm" color="gray.600">
              You are signed in.
            </Text>
          </Box>
        </SimpleGrid>
      </VStack>
    </Flex>
  );
}

export default HomePage;
