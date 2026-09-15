import { 
  fetchAvailableSlots, 
  scheduleAppointment 
} from '../api';
import { slotSelectionSchema } from '../validation';

import { vi } from 'vitest';

describe('API Functions', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('fetchAvailableSlots', () => {
    it('fetches slots with correct parameters', async () => {
      const mockResponse = {
        date: '2026-09-11',
        slotStart: '09:00',
        slotEnd: '09:30',
        doctorId: 123,
      };
      
      (fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse),
      });

      const result = await fetchAvailableSlots({ doctorId: 123, date: '2026-09-11' });
      expect(result).toEqual(slotSelectionSchema.parse(mockResponse));
      expect(fetch).toHaveBeenCalledWith(
        '/api/opd/slots?doctorId=123&date=2026-09-11'
      );
    });

    it('throws on invalid response', async () => {
      (fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({}),
      });

      await expect(fetchAvailableSlots({ doctorId: 123, date: '2026-09-11' }))
        .rejects
        .toThrow();
    });
  });

  describe('scheduleAppointment', () => {
    it('sends POST request with correct payload', async () => {
      const mockPayload = {
        followUpDate: '2026-09-11',
        slotStart: '09:00',
        slotEnd: '09:30',
      };
      
      (fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ id: 456 }),
      });

      await scheduleAppointment(mockPayload);
      expect(fetch).toHaveBeenCalledWith(
        '/api/opd/appointments',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(mockPayload),
        })
      );
    });
  });
});