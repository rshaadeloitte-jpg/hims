import type { Prescription } from "./types";
import { Box, Button } from "@chakra-ui/react";

interface DispenseFormProps {
  prescription: Prescription;
  onClose: () => void;
}

export function DispenseForm({ onClose }: DispenseFormProps) {
  return (
    <Box mt={6} p={4} bg="gray.50" borderRadius="lg">
      <p className="text-sm text-amber-600">
        Dispensing a prescription is not supported by the backend.
      </p>
      <Button mt={3} variant="secondary" onClick={onClose}>
        Close
      </Button>
    </Box>
  );
}
