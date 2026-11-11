import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, SimpleGrid, Textarea, VStack, Text } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { patientRegistrationSchema, type PatientRegistrationValues } from "./zodSchema";
import { useRegisterPatient } from "./queries";

/**
 * M02 Patient Registration form (Registration Form, G-1). Wraps react-hook-form
 * + zod and posts to patient-service /api/v1/patients. The legacy backend is
 * tolerant of partial payloads (PatientRegApptServiceImpl parity) so we
 * forward every defined field and let the server handle nulls.
 */
export function PatientRegistrationForm({ onCreated }: { onCreated?: (id: number) => void }) {
  const mutation = useRegisterPatient();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<PatientRegistrationValues>({
    resolver: zodResolver(patientRegistrationSchema),
    defaultValues: {
      firstName: "",
      middleName: "",
      lastName: "",
      mobileNo: "",
      abhaAddress: "",
      address: "",
      email: "",
      emergencyContact: "",
      bloodGroup: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    const payload: Record<string, unknown> = {};
    if (values.firstName) payload.firstName = values.firstName;
    if (values.middleName) payload.middleName = values.middleName;
    if (values.lastName) payload.lastName = values.lastName;
    if (values.mobileNo) payload.mobileNo = values.mobileNo;
    if (values.email) payload.email = values.email;
    if (values.genderId != null) payload.gender = values.genderId;
    if (values.dateOfBirth) payload.dob = values.dateOfBirth;
    if (values.bloodGroup) payload.bloodGroup = values.bloodGroup;
    if (values.maritalStatus) payload.maritalStatus = values.maritalStatus;
    if (values.address) payload.addressLine1 = values.address;
    if (values.city) payload.city = values.city;
    if (values.state) payload.state = values.state;
    if (values.pincode) payload.pincode = values.pincode;
    if (values.abhaAddress) payload.healthIdNumber = values.abhaAddress;
    if (values.aadhaarNumber) payload.aadhaarNo = values.aadhaarNumber;
    if (values.unitId != null) payload.unitId = values.unitId;
    if (values.emergencyContact) payload.emergencyContactPhone = values.emergencyContact;
    const created = await mutation.mutateAsync(payload);
    onCreated?.(created.patientId);
    reset();
  });

  return (
    <form onSubmit={onSubmit} aria-label="patient-registration">
      <VStack align="stretch" gap={3} p={4} borderWidth="1px" borderColor="gray.200" borderRadius="md">
        <Text fontSize="lg" fontWeight="semibold">Patient Registration</Text>

        <SimpleGrid columns={{ base: 1, sm: 3 }} gap={3}>
          <FormField label="First name" required error={errors.firstName?.message}>
            <TextInput autoComplete="given-name" aria-required="true" {...register("firstName")} />
          </FormField>
          <FormField label="Middle name" error={errors.middleName?.message}>
            <TextInput autoComplete="additional-name" {...register("middleName")} />
          </FormField>
          <FormField label="Last name" required error={errors.lastName?.message}>
            <TextInput autoComplete="family-name" aria-required="true" {...register("lastName")} />
          </FormField>
        </SimpleGrid>

        <SimpleGrid columns={{ base: 1, sm: 2 }} gap={3}>
          <FormField label="Mobile" required error={errors.mobileNo?.message}>
            <TextInput inputMode="tel" autoComplete="tel" aria-required="true" {...register("mobileNo")} />
          </FormField>
          <FormField label="ABHA address" error={errors.abhaAddress?.message}>
            <TextInput autoComplete="username" {...register("abhaAddress")} />
          </FormField>
        </SimpleGrid>

        <SimpleGrid columns={{ base: 1, sm: 3 }} gap={3}>
          <FormField label="Date of birth" error={errors.dateOfBirth?.message}>
            <TextInput type="date" {...register("dateOfBirth")} />
          </FormField>
          <FormField label="Gender ID" error={errors.genderId?.message}>
            <TextInput type="number" {...register("genderId")} />
          </FormField>
          <FormField label="Blood group" error={errors.bloodGroup?.message}>
            <HimsSelect {...register("bloodGroup")}>
              <option value="">Select</option>
              {["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"].map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </HimsSelect>
          </FormField>
        </SimpleGrid>

        <FormField label="Address" error={errors.address?.message}>
          <Textarea bg="white" borderColor="gray.300" rows={2} autoComplete="street-address" {...register("address")} />
        </FormField>

        <SimpleGrid columns={{ base: 1, sm: 2 }} gap={3}>
          <FormField label="Email" error={errors.email?.message}>
            <TextInput type="email" autoComplete="email" {...register("email")} />
          </FormField>
          <FormField label="Emergency contact" error={errors.emergencyContact?.message}>
            <TextInput inputMode="numeric" {...register("emergencyContact")} />
          </FormField>
        </SimpleGrid>

        {mutation.isError && (
          <Text color="red.600" fontSize="sm">{(mutation.error as Error).message}</Text>
        )}
        {mutation.isSuccess && (
          <Text color="green.600" fontSize="sm">Registered. Patient ID: {mutation.data.patientId}</Text>
        )}

        <Button
          type="submit"
          variant="primary"
          disabled={isSubmitting}
          loading={mutation.isPending}
          loadingText="Saving..."
        >
          Register Patient
        </Button>
      </VStack>
    </form>
  );
}
