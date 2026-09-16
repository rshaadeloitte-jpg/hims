import { useState } from 'react';
import { Button, Checkbox, Table } from '@chakra-ui/react';
import { HimsSelect, TextInput } from "../../components/ui";
import { useLabTests } from './useLabTests';
import { useCreateLabOrder } from './queries';

interface TestBookingProps {
  patientId: number;
  encounterId?: number;
  onBooked?: (orderId: number) => void;
}

/**
 * M15 Test Booking - Linear-style data table for test selection
 */
export function TestBooking({ patientId, encounterId, onBooked }: TestBookingProps) {
  const [selectedTests, setSelectedTests] = useState<Set<number>>(new Set());
  const [searchQuery, setSearchQuery] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState<string>('');
  
  const { data: tests = [], isLoading } = useLabTests({
    search: searchQuery || undefined,
    department: departmentFilter || undefined,
  });
  
  const createOrder = useCreateLabOrder();

  const toggleTest = (testId: number) => {
    setSelectedTests(prev => {
      const next = new Set(prev);
      if (next.has(testId)) {
        next.delete(testId);
      } else {
        next.add(testId);
      }
      return next;
    });
  };

  const handleBook = async () => {
    if (selectedTests.size === 0) return;

    const testList = Array.from(selectedTests).map(testId => {
      const test = tests.find(t => t.testId === testId);
      return {
        testId: test?.testId,
        testName: test?.testName,
        testCode: test?.testCode,
        rate: test?.rate,
      };
    });

    try {
      const order = await createOrder.mutateAsync({
        patientId,
        encounterId,
        tests: testList,
        isOutSourced: false,
      });
      
      setSelectedTests(new Set());
      onBooked?.(order.orderId);
    } catch (err) {
      console.error('Failed to create order:', err);
    }
  };

  const departments = Array.from(new Set(tests.map(t => t.department))).sort();

  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Book Lab Tests</h2>
        <Button
          variant="primary"
          size="sm"
          onClick={handleBook}
          loading={createOrder.isPending}
          loadingText="Booking..."
          disabled={selectedTests.size === 0 || createOrder.isPending}
        >
          {createOrder.isPending ? 'Booking...' : `Book ${selectedTests.size} Test${selectedTests.size !== 1 ? 's' : ''}`}
        </Button>
      </header>

      {/* Filters */}
      <div className="flex gap-3">
        <TextInput
          type="text"
          placeholder="Search tests..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        <HimsSelect
          value={departmentFilter}
          onChange={(e) => setDepartmentFilter(e.target.value)}
        >
          <option value="">All Departments</option>
          {departments.map(dept => (
            <option key={dept} value={dept}>{dept}</option>
          ))}
        </HimsSelect>
      </div>

      {/* Linear-style Data Table */}
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">
                  <Checkbox.Root
                    checked={selectedTests.size === tests.length && tests.length > 0}
                    onCheckedChange={(e) => {
                      if (e.checked) {
                        setSelectedTests(new Set(tests.map(t => t.testId)));
                      } else {
                        setSelectedTests(new Set());
                      }
                    }}
                  >
                    <Checkbox.Control />
                  </Checkbox.Root>
                </Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Test Name</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Department</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Sample</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">TAT</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Rate</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {isLoading ? (
                <Table.Row>
                  <Table.Cell colSpan={7}>
                    Loading tests...
                  </Table.Cell>
                </Table.Row>
              ) : tests.length === 0 ? (
                <Table.Row>
                  <Table.Cell colSpan={7}>
                    No tests found
                  </Table.Cell>
                </Table.Row>
              ) : (
                tests.map((test) => (
                  <Table.Row
                    key={test.testId}
                    onClick={() => toggleTest(test.testId)}
                  >
                    <Table.Cell>
                      <Checkbox.Root
                        checked={selectedTests.has(test.testId)}
                        onCheckedChange={() => toggleTest(test.testId)}
                      >
                        <Checkbox.Control />
                      </Checkbox.Root>
                    </Table.Cell>
                    <Table.Cell>{test.testCode}</Table.Cell>
                    <Table.Cell>{test.testName}</Table.Cell>
                    <Table.Cell>{test.department}</Table.Cell>
                    <Table.Cell>
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
                        {test.sampleType}
                      </span>
                    </Table.Cell>
                    <Table.Cell>
                      {test.turnaroundTime}h
                    </Table.Cell>
                    <Table.Cell>
                      ₹{test.rate.toFixed(2)}
                    </Table.Cell>
                  </Table.Row>
                ))
              )}
            </Table.Body>
          </Table.Root>
        </div>
      </div>

      {createOrder.isError && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-md">
          <p className="text-sm text-red-800">
            Failed to create order: {(createOrder.error as Error).message}
          </p>
        </div>
      )}
    </section>
  );
}