import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PatientBanner } from "./PatientBanner";
import { opSamples } from "../__tests__/mocks/handlers";

describe("PatientBanner", () => {
  it("renders MRN, name, sex, and age in the legacy facesheet header layout", () => {
    render(<PatientBanner patient={opSamples.patient} />);
    expect(screen.getByText("MRN-7001")).toBeInTheDocument();
    expect(screen.getByText("Asha Verma")).toBeInTheDocument();
    expect(screen.getByText("F")).toBeInTheDocument();
    expect(screen.getByText("33y")).toBeInTheDocument();
    expect(screen.getByText("📞 +91 98765 43210")).toBeInTheDocument();
  });

  it("shows ABHA pill when the patient has a linked ABHA id", () => {
    render(<PatientBanner patient={opSamples.patient} />);
    expect(screen.getByText("ABHA: 12-3456-7890-1234")).toBeInTheDocument();
  });

  it("falls back to t-MRN (pending) when isTempMrn is true", () => {
    render(<PatientBanner patient={{ ...opSamples.patient, mrn: undefined, isTempMrn: true }} />);
    expect(screen.getByText("t-MRN (pending)")).toBeInTheDocument();
  });

  it("renders the rightSlot when supplied (e.g. ABHA badge)", () => {
    render(
      <PatientBanner
        patient={opSamples.patient}
        rightSlot={<span data-testid="right">ABHA: linked</span>}
      />,
    );
    expect(screen.getByTestId("right")).toBeInTheDocument();
  });

  it("invokes onClick when interactive", () => {
    const onClick = vi.fn();
    render(<PatientBanner patient={opSamples.patient} onClick={onClick} />);
    fireEvent.click(screen.getByRole("button", { name: /open patient Asha Verma/i }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("supports keyboard activation (Enter)", () => {
    const onClick = vi.fn();
    render(<PatientBanner patient={opSamples.patient} onClick={onClick} />);
    fireEvent.keyDown(screen.getByRole("button", { name: /open patient Asha Verma/i }), {
      key: "Enter",
    });
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
