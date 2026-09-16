export interface NavItem {
  to: string;
  label: string;
  /** Match only exact path (default: prefix match) */
  end?: boolean;
}

export interface NavSection {
  id: string;
  title: string;
  icon: string;
  items: NavItem[];
}

/**
 * Single source of truth for sidebar navigation.
 * Prefix matching is used so `/billing/new` still highlights "Bills".
 */
export const NAV_SECTIONS: NavSection[] = [
  {
    id: "overview",
    title: "Overview",
    icon: "grid",
    items: [
      { to: "/masters", label: "Dashboard" },
      { to: "/queue", label: "Queue Dashboard" },
      { to: "/reports/dashboard", label: "Reports Dashboard" },
      { to: "/billing/dashboard", label: "Billing Dashboard" },
    ],
  },
  {
    id: "front-office",
    title: "Front Office",
    icon: "calendar",
    items: [
      { to: "/patients/register", label: "Patient Registration" },
      { to: "/patients", label: "Patient Search", end: true },
      { to: "/m/op/walkinVisit", label: "Walk-in Visit" },
      { to: "/m/op/addAppointment", label: "New Appointment" },
      { to: "/m/op/appointmentList", label: "Appointments" },
      { to: "/m/op/abha", label: "ABHA Services" },
      { to: "/m02/walkin", label: "Walk-in Desk" },
      { to: "/m02/booking", label: "Follow-up Booking" },
      { to: "/m02/followups", label: "Follow-ups" },
    ],
  },
  {
    id: "clinical",
    title: "Clinical",
    icon: "activity",
    items: [
      { to: "/nursing", label: "Nursing Care", end: true },
      { to: "/nursing/worklist", label: "Nursing Worklist" },
      { to: "/lab", label: "Laboratory" },
      { to: "/radiology", label: "Radiology" },
      { to: "/orders", label: "Doctor Orders" },
      { to: "/bcma", label: "BCMA" },
      { to: "/ot", label: "Operation Theatre" },
      { to: "/bloodbank", label: "Blood Bank" },
      { to: "/diet", label: "Diet & Kitchen" },
      { to: "/cssd", label: "CSSD" },
      { to: "/chemotherapy", label: "Chemotherapy" },
      { to: "/mortuary", label: "Mortuary" },
    ],
  },
  {
    id: "billing",
    title: "Billing & Finance",
    icon: "receipt",
    items: [
      { to: "/billing", label: "Bills", end: true },
      { to: "/billing/new", label: "New Bill" },
      { to: "/tariffs", label: "Tariffs" },
      { to: "/doctor-share", label: "Doctor Share" },
      { to: "/rcm", label: "RCM Claims" },
      { to: "/payment-transfer", label: "Payment Transfer" },
      { to: "/procurement", label: "Procurement" },
    ],
  },
  {
    id: "pharmacy",
    title: "Pharmacy & Stores",
    icon: "pill",
    items: [
      { to: "/pharmacy", label: "Pharmacy", end: true },
      { to: "/pharmacy/new", label: "New Sale" },
      { to: "/pharmacy/returns", label: "Sale Returns" },
      { to: "/pharmacy/safety", label: "Drug Safety" },
      { to: "/inventory", label: "Inventory" },
      { to: "/equipment", label: "Equipment" },
      { to: "/linen", label: "Linen" },
    ],
  },
  {
    id: "operations",
    title: "Operations",
    icon: "bed",
    items: [
      { to: "/adt", label: "ADT / Bed Board" },
      { to: "/transport", label: "Transport" },
      { to: "/grievance", label: "Grievance" },
      { to: "/worklists", label: "Worklists" },
      { to: "/pmrd", label: "Medical Records" },
      { to: "/rmrs", label: "RMRS Referrals" },
      { to: "/bmw", label: "BMW Waste" },
      { to: "/files", label: "Files & Documents" },
    ],
  },
  {
    id: "digital",
    title: "Digital & Kiosk",
    icon: "monitor",
    items: [
      { to: "/kiosk", label: "Kiosk Home", end: true },
      { to: "/kiosk/self-register", label: "Kiosk Registration" },
      { to: "/kiosk/appointment", label: "Kiosk Appointment" },
      { to: "/kiosk/lab-result", label: "Lab Result Upload" },
      { to: "/aasmaan", label: "Aasmaan Services" },
      { to: "/interop", label: "Interop / FHIR" },
      { to: "/ejs", label: "EJS / ASHA" },
    ],
  },
  {
    id: "admin",
    title: "Masters & Admin",
    icon: "settings",
    items: [
      { to: "/masters", label: "Master Data", end: true },
      { to: "/forms", label: "Forms", end: true },
      { to: "/forms/submissions", label: "Form Submissions" },
      { to: "/reports", label: "Reports", end: true },
      { to: "/reports/executions", label: "Report Runs" },
      { to: "/commons/config", label: "App Config" },
      { to: "/commons/audit", label: "Audit Trail" },
      { to: "/commons/hospital", label: "Hospital Setup" },
      { to: "/auth/admin", label: "User Admin" },
    ],
  },
];

/** Flat list used by the header search. */
export const NAV_FLAT: Array<NavItem & { section: string }> = NAV_SECTIONS.flatMap(
  (section) => section.items.map((item) => ({ ...item, section: section.title })),
);
