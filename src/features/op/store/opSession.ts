import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

/**
 * OP session state (Zustand) — the slice of cross-cutting state the OP screens
 * share. Mirrors the legacy Fg.g() / localStorage pattern (09-react-migration
 * §1 — Zustand for "session/global: unit_id, duz, isNhnc, app_type_w, route flow
 * counters, printer prefs = legacy Fg.g()/localStorage equivalents").
 *
 * `opSession` is persisted to localStorage so the walk-in / appointment flows
 * survive page refresh (the legacy webapp behaved similarly via Fg.g() cookies).
 * Persisted fields are intentionally small — they should survive reload, not
 * grow with usage.
 */
export interface OpSession {
  /** The unit the user is signed in for; legacy Fg.g("UnitId") equivalent. */
  unitId: number | null;
  /** The DUZ (Doctor User Zone) id; legacy Fg.g("Duz") equivalent. */
  duz: number | null;
  /** NHNC scheme flag (09 §1 — "isNhnc") — affects many flow rules. */
  isNhnc: boolean;
  /** App type: "OP" | "IP" | "EMG" — legacy Fg.g("AppType") equivalent. */
  appType: "OP" | "IP" | "EMG";
  /** Default counter for queue issuance; legacy Fg.g("CounterId") equivalent. */
  defaultCounterId: number | null;
  /** Walk-in form draft — survives page refresh, the patient is still mid-form. */
  walkinDraft: WalkinDraft | null;
}

export interface WalkinDraft {
  // step-1 patient demographic fields
  firstName?: string;
  middleName?: string;
  lastName?: string;
  fatherName?: string;
  motherName?: string;
  dateOfBirth?: string;
  age?: string;
  sex?: "M" | "F" | "O" | "U";
  mobile?: string;
  aadhaar?: string;
  // step-2 MLC
  mlcNumber?: string;
  mlcFirNumber?: string;
  mlcPoliceStation?: string;
  mlcNotes?: string;
  // step-3 sponsor
  sponsorId?: number;
  schemeCode?: string;
  // step-4 emergency / referral
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  referralDoctorId?: number;
  notes?: string;
  // timestamps for "how stale is this draft"
  updatedAt?: string;
}

interface OpActions {
  setUnit: (unitId: number | null) => void;
  setDuz: (duz: number | null) => void;
  setIsNhnc: (nhnc: boolean) => void;
  setAppType: (appType: OpSession["appType"]) => void;
  setDefaultCounter: (counterId: number | null) => void;
  saveWalkinDraft: (patch: Partial<WalkinDraft>) => void;
  clearWalkinDraft: () => void;
}

const initialState: OpSession = {
  unitId: null,
  duz: null,
  isNhnc: false,
  appType: "OP",
  defaultCounterId: null,
  walkinDraft: null,
};

/**
 * Hydration helper — runs once at module import. Zustand's persist middleware
 * handles the actual localStorage round-trip; this exists so unit tests can
 * reset between cases (see op/__tests__/setup.ts).
 */
export const useOpSession = create<OpSession & OpActions>()(
  persist(
    (set) => ({
      ...initialState,
      setUnit: (unitId) => set({ unitId }),
      setDuz: (duz) => set({ duz }),
      setIsNhnc: (isNhnc) => set({ isNhnc }),
      setAppType: (appType) => set({ appType }),
      setDefaultCounter: (defaultCounterId) => set({ defaultCounterId }),
      saveWalkinDraft: (patch) =>
        set((s) => ({
          walkinDraft: {
            ...(s.walkinDraft ?? {}),
            ...patch,
            updatedAt: new Date().toISOString(),
          },
        })),
      clearWalkinDraft: () => set({ walkinDraft: null }),
    }),
    {
      name: "ihms-op-session",
      storage: createJSONStorage(() => localStorage),
      // persist only the legacy-equivalent session fields; skip the walkinDraft
      // when running in tests (the persist hook reads the env via storage).
      partialize: (state) => ({
        unitId: state.unitId,
        duz: state.duz,
        isNhnc: state.isNhnc,
        appType: state.appType,
        defaultCounterId: state.defaultCounterId,
        walkinDraft: state.walkinDraft,
      }),
    },
  ),
);

/** Reset the store to initial state (test setup helper). */
export function resetOpSession(): void {
  // Preserve the action methods (which Zustand attaches to the store) and
  // just reset the data fields. Without this, `useOpSession.getState()` after
  // the reset would return a state that lacks setUnit / saveWalkinDraft etc.
  const s = useOpSession.getState();
  s.setUnit(null);
  s.setDuz(null);
  s.setIsNhnc(false);
  s.setAppType("OP");
  s.setDefaultCounter(null);
  s.clearWalkinDraft();
}
