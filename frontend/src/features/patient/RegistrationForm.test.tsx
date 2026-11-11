import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RegistrationForm } from "./RegistrationForm";
import { PatientRegistrationForm } from "./PatientRegistrationForm";

function renderWithClient(ui: React.ReactElement) {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/patients/register"]}>
        {ui}
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("RegistrationForm", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((url: string) => {
        if (url.includes("/api/v1/patients/register")) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                id: 501,
                patientId: 501,
                mrno: "202611000000501",
                mrNo: "202611000000501",
                firstName: "Aarav",
                lastName: "Sharma",
                mobileNo: "9876543210",
              }),
              { status: 201, headers: { "Content-Type": "application/json" } },
            ),
          );
        }
        return Promise.resolve(new Response(JSON.stringify({}), { status: 200 }));
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows success confirmation state with patient name and MRN after successful registration", async () => {
    renderWithClient(<RegistrationForm />);

    // Step 1: Identity
    const firstNameInput = screen.getByLabelText(/First name/i);
    fireEvent.change(firstNameInput, { target: { value: "Aarav" } });
    const lastNameInput = screen.getByLabelText(/Last name/i);
    fireEvent.change(lastNameInput, { target: { value: "Sharma" } });

    // Go to contact
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));

    // Step 2: Contact
    const mobileInput = await screen.findByLabelText(/Mobile/i);
    fireEvent.change(mobileInput, { target: { value: "9876543210" } });

    // Jump to covid (last step)
    fireEvent.click(screen.getByRole("button", { name: /COVID-19/i }));

    // Submit form
    const submitBtn = await screen.findByRole("button", { name: /Register Patient/i });
    fireEvent.click(submitBtn);

    // Expect success confirmation state
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /Patient Registered Successfully/i })).toBeInTheDocument();
    });

    expect(screen.getByTestId("registered-patient-name")).toHaveTextContent("Aarav Sharma");
    expect(screen.getByTestId("registered-patient-mrn")).toHaveTextContent("202611000000501");
    expect(screen.getByTestId("registered-patient-id")).toHaveTextContent("501");
    expect(screen.getByRole("button", { name: /Register Another Patient/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Go to Patient Profile/i })).toBeInTheDocument();
  });

  it("resets cleanly without validation errors when 'Register Another Patient' is clicked", async () => {
    renderWithClient(<RegistrationForm />);

    // Fill and submit
    fireEvent.change(screen.getByLabelText(/First name/i), { target: { value: "Aarav" } });
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    const mobileInput = await screen.findByLabelText(/Mobile/i);
    fireEvent.change(mobileInput, { target: { value: "9876543210" } });
    fireEvent.click(screen.getByRole("button", { name: /COVID-19/i }));
    fireEvent.click(await screen.findByRole("button", { name: /Register Patient/i }));

    // Wait for success screen
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /Patient Registered Successfully/i })).toBeInTheDocument();
    });

    // Click Register Another
    fireEvent.click(screen.getByRole("button", { name: /Register Another Patient/i }));

    // Must be back on section 1 (Identity)
    expect(screen.getByLabelText(/First name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/First name/i)).toHaveValue("");

    // Must NOT surface validation errors on the reset form
    expect(screen.queryByText(/First name is required/i)).not.toBeInTheDocument();
  });
});

describe("PatientRegistrationForm", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((url: string) => {
        if (url.includes("/api/v1/patients/register")) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                id: 888,
                patientId: 888,
                mrno: "202611000000888",
                mrNo: "202611000000888",
                firstName: "Priya",
                lastName: "Patel",
                mobileNo: "9123456780",
              }),
              { status: 201, headers: { "Content-Type": "application/json" } },
            ),
          );
        }
        return Promise.resolve(new Response(JSON.stringify({}), { status: 200 }));
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows success confirmation state with patient name and MRN", async () => {
    renderWithClient(<PatientRegistrationForm />);

    fireEvent.change(screen.getByLabelText(/First name/i), { target: { value: "Priya" } });
    fireEvent.change(screen.getByLabelText(/Last name/i), { target: { value: "Patel" } });
    fireEvent.change(screen.getByLabelText(/Mobile/i), { target: { value: "9123456780" } });

    fireEvent.click(screen.getByRole("button", { name: /Register Patient/i }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /Patient Registered Successfully/i })).toBeInTheDocument();
    });

    expect(screen.getByTestId("registered-patient-name")).toHaveTextContent("Priya");
    expect(screen.getByTestId("registered-patient-mrn")).toHaveTextContent("202611000000888");

    // Click register another
    fireEvent.click(screen.getByRole("button", { name: /Register Another Patient/i }));
    expect(screen.getByLabelText(/First name/i)).toHaveValue("");
    expect(screen.queryByText(/First name is required/i)).not.toBeInTheDocument();
  });
});
