import React from 'react';

interface Bed {
  id: string;
  number: string;
  ward: string;
  status: 'occupied' | 'available' | 'maintenance';
  patientName?: string;
  admissionDate?: string;
}

const STATUS_BADGE_CLASSES = {
  occupied: 'bg-rose-100 text-rose-800 ring-rose-600/20',
  available: 'bg-emerald-100 text-emerald-800 ring-emerald-600/20',
  maintenance: 'bg-amber-100 text-amber-800 ring-amber-600/20',
} as const;

export const BedBoard = () => {
  const { beds, loading, error } = useBedBoardData();

  if (error) return <div className="text-center py-12">Error loading bed data</div>;
  if (loading) return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {[...Array(6)].map((_, i) => (
        <div key={i} className="h-24 bg-muted animate-pulse rounded-lg" />
      ))}
    </div>
  );

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {beds.map((bed) => (
        <div
          key={bed.id}
          className="border rounded-lg p-4 hover:shadow-md transition-shadow"
        >
          <div className="flex justify-between items-start">
            <div>
              <h3 className="font-medium text-gray-900">Bed {bed.number}</h3>
              <p className="text-sm text-gray-500">{bed.ward}</p>
            </div>
            <span className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset ${STATUS_BADGE_CLASSES[bed.status]}`}>
              {bed.status === 'occupied' && 'Occupied'}
              {bed.status === 'available' && 'Available'}
              {bed.status === 'maintenance' && 'Maintenance'}
            </span>
          </div>
          
          {bed.status === 'occupied' && (
            <div className="mt-3 pt-3 border-t">
              <p className="text-sm font-medium text-gray-900">{bed.patientName}</p>
              <p className="text-xs text-gray-500">Admitted: {bed.admissionDate}</p>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

const useBedBoardData = () => {
  // In a real implementation, this would fetch from an API
  const mockBeds: Bed[] = [
    { id: '1', number: '101', ward: 'ICU', status: 'occupied', patientName: 'John Doe', admissionDate: '2023-09-10' },
    { id: '2', number: '102', ward: 'ICU', status: 'available' },
    { id: '3', number: '201', ward: 'Cardiology', status: 'maintenance' },
    { id: '4', number: '202', ward: 'Cardiology', status: 'occupied', patientName: 'Jane Smith', admissionDate: '2023-09-09' },
  ];

  return {
    beds: mockBeds,
    loading: false,
    error: null,
  };
};