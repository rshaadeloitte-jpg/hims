import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import {
  AppointmentStatusBadge,
  AbhaStatusBadge,
  QueueStatusBadge,
  QueuePriorityBadge,
  YesNoBadge,
} from "./StatusBadges";

describe("AppointmentStatusBadge", () => {
  it.each([
    [1, /Booked/],
    [2, /Checked-In/],
    [3, /Completed/],
    [4, /Cancelled/],
    [5, /Rescheduled/],
    [6, /No-Show/],
  ] as const)("status %s → %s", (status, labelRe) => {
    render(<AppointmentStatusBadge status={status} />);
    expect(screen.getByText(labelRe)).toBeInTheDocument();
  });
});

describe("AbhaStatusBadge", () => {
  it.each([
    ["PENDING_OTP", /Pending OTP/],
    ["OTP_VERIFIED", /OTP Verified/],
    ["LINKED", /ABHA Linked/],
    ["FAILED", /Failed/],
  ] as const)("status %s → %s", (status, labelRe) => {
    render(<AbhaStatusBadge status={status as never} />);
    expect(screen.getByText(labelRe)).toBeInTheDocument();
  });
});

describe("QueueStatusBadge", () => {
  it.each([
    ["WAITING"],
    ["IN_PROGRESS"],
    ["COMPLETED"],
    ["CANCELLED"],
    ["NO_SHOW"],
    ["TRANSFERRED"],
  ] as const)("renders %s", (status) => {
    render(<QueueStatusBadge status={status as never} />);
    expect(screen.getByText(new RegExp(status.replace("_", " ")))).toBeInTheDocument();
  });
});

describe("QueuePriorityBadge", () => {
  it("renders the priority label", () => {
    render(<QueuePriorityBadge priority="EMERGENCY" />);
    expect(screen.getByText("EMERGENCY")).toBeInTheDocument();
  });
});

describe("YesNoBadge", () => {
  it("renders Yes when true", () => {
    render(<YesNoBadge value={true} />);
    expect(screen.getByText("Yes")).toBeInTheDocument();
  });
  it("renders No when false (default label)", () => {
    render(<YesNoBadge value={false} />);
    expect(screen.getByText("No")).toBeInTheDocument();
  });
  it("honors custom labels", () => {
    render(<YesNoBadge value={true} yes="Active" no="Inactive" />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });
});
