import { slotSelectionSchema, appointmentSchema } from '../validation';

describe('Slot Selection Schema', () => {
  it('validates correct slot selection data', () => {
    const validData = {
      date: '2026-09-11',
      slotStart: '09:00',
      slotEnd: '09:30',
      doctorId: 123,
    };
    expect(() => slotSelectionSchema.parse(validData)).not.toThrow();
  });

  it('rejects invalid date format', () => {
    const invalidData = {
      date: '11-09-2026',
      slotStart: '09:00',
      slotEnd: '09:30',
      doctorId: 123,
    };
    expect(() => slotSelectionSchema.parse(invalidData)).toThrow();
  });

  it('rejects negative doctor ID', () => {
    const invalidData = {
      date: '2026-09-11',
      slotStart: '09:00',
      slotEnd: '09:30',
      doctorId: -1,
    };
    expect(() => slotSelectionSchema.parse(invalidData)).toThrow();
  });
});

describe('Appointment Schema', () => {
  it('validates correct appointment data', () => {
    const validData = {
      followUpDate: '2026-09-11',
      doctorId: 123,
      slotStart: '09:00',
      slotEnd: '09:30',
    };
    expect(() => appointmentSchema.parse(validData)).not.toThrow();
  });

  it('accepts optional cabin field within limits', () => {
    const validData = {
      followUpDate: '2026-09-11',
      doctorId: 123,
      slotStart: '09:00',
      slotEnd: '09:30',
      cabin: 'A-101',
    };
    expect(() => appointmentSchema.parse(validData)).not.toThrow();
  });

  it('rejects cabin field exceeding 20 characters', () => {
    const invalidData = {
      followUpDate: '2026-09-11',
      doctorId: 123,
      slotStart: '09:00',
      slotEnd: '09:30',
      cabin: 'A-101-This-Is-Too-Long',
    };
    expect(() => appointmentSchema.parse(invalidData)).toThrow();
  });
});