import { describe, expect, it, beforeEach } from "vitest";
import { useOpSession, resetOpSession } from "../store/opSession";

describe("useOpSession (Zustand)", () => {
  beforeEach(() => {
    resetOpSession();
  });

  it("starts with the documented defaults", () => {
    const s = useOpSession.getState();
    expect(s.unitId).toBeNull();
    expect(s.duz).toBeNull();
    expect(s.isNhnc).toBe(false);
    expect(s.appType).toBe("OP");
    expect(s.defaultCounterId).toBeNull();
    expect(s.walkinDraft).toBeNull();
  });

  it("setUnit / setDuz / setIsNhnc / setAppType / setDefaultCounter all update", () => {
    const s = useOpSession.getState();
    s.setUnit(42);
    s.setDuz(9001);
    s.setIsNhnc(true);
    s.setAppType("EMG");
    s.setDefaultCounter(7);
    const s2 = useOpSession.getState();
    expect(s2.unitId).toBe(42);
    expect(s2.duz).toBe(9001);
    expect(s2.isNhnc).toBe(true);
    expect(s2.appType).toBe("EMG");
    expect(s2.defaultCounterId).toBe(7);
  });

  it("saveWalkinDraft merges and stamps updatedAt", async () => {
    const s = useOpSession.getState();
    s.saveWalkinDraft({ firstName: "Asha", lastName: "Verma" });
    let draft = useOpSession.getState().walkinDraft;
    expect(draft?.firstName).toBe("Asha");
    expect(draft?.lastName).toBe("Verma");
    expect(draft?.updatedAt).toBeDefined();
    // Subsequent saves merge
    useOpSession.getState().saveWalkinDraft({ mobile: "+91 98765 43210" });
    draft = useOpSession.getState().walkinDraft;
    expect(draft?.firstName).toBe("Asha");
    expect(draft?.mobile).toBe("+91 98765 43210");
    // updatedAt is updated each time
    const t1 = new Date(draft!.updatedAt!).getTime();
    await new Promise((r) => setTimeout(r, 5));
    useOpSession.getState().saveWalkinDraft({ lastName: "Iyer" });
    const t2 = new Date(useOpSession.getState().walkinDraft!.updatedAt!).getTime();
    expect(t2).toBeGreaterThanOrEqual(t1);
  });

  it("clearWalkinDraft wipes the draft", () => {
    const s = useOpSession.getState();
    s.saveWalkinDraft({ firstName: "Asha" });
    expect(useOpSession.getState().walkinDraft).not.toBeNull();
    useOpSession.getState().clearWalkinDraft();
    expect(useOpSession.getState().walkinDraft).toBeNull();
  });
});
