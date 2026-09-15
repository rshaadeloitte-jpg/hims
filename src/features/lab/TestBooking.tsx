import { useState } from 'react';
import { useLabTests } from './useLabTests';
import { useCreateLabOrder } from './queries';
import type { LabTest } from './types';

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
        <button
          onClick={handleBook}
          disabled={selectedTests.size === 0 || createOrder.isPending}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {createOrder.isPending ? 'Booking...' : `Book ${selectedTests.size} Test${selectedTests.size !== 1 ? 's' : ''}`}
        </button>
      </header>

      {/* Filters */}
      <div className="flex gap-3">
        <input
          type="text"
          placeholder="Search tests..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
        />
        <select
          value={departmentFilter}
          onChange={(e) => setDepartmentFilter(e.target.value)}
          className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
        >
          <option value="">All Departments</option>
          {departments.map(dept => (
            <option key={dept} value={dept}>{dept}</option>
          ))}
        </select>
      </div>

      {/* Linear-style Data Table */}
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                  <input
                    type="checkbox"
                    checked={selectedTests.size === tests.length && tests.length > 0}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedTests(new Set(tests.map(t => t.testId)));
                      } else {
                        setSelectedTests(new Set());
                      }
                    }}
                    className="rounded border-gray-300"
                  />
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Code</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Test Name</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Department</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Sample</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">TAT</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Rate</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-gray-500">
                    Loading tests...
                  </td>
                </tr>
              ) : tests.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-gray-500">
                    No tests found
                  </td>
                </tr>
              ) : (
                tests.map((test) => (
                  <tr
                    key={test.testId}
                    onClick={() => toggleTest(test.testId)}
                    className={`cursor-pointer transition-colors ${
                      selectedTests.has(test.testId)
                        ? 'bg-blue-50 hover:bg-blue-100'
                        : 'hover:bg-gray-50'
                    }`}
                  >
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        checked={selectedTests.has(test.testId)}
                        onChange={() => toggleTest(test.testId)}
                        className="rounded border-gray-300"
                        onClick={(e) => e.stopPropagation()}
                      />
                    </td>
                    <td className="px-4 py-3 font-mono text-sm text-gray-900">{test.testCode}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{test.testName}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{test.department}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
                        {test.sampleType}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 text-right font-mono">
                      {test.turnaroundTime}h
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 text-right font-mono">
                      ₹{test.rate.toFixed(2)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
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