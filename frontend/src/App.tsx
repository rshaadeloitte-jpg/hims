import { Link, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout";
import { MasterEditPage } from "./features/masters/MasterEditPage";
import { MasterListPage } from "./features/masters/MasterListPage";
import { FormListPage } from "./features/forms/FormListPage";
import {
  AbhaPage,
  AddAppointmentPage,
  AppointmentListPage,
  WalkinVisitPage,
} from "./features/op";
import { AppointmentBooking } from "./features/opd/AppointmentBooking";
import { ConsultationDesk } from "./features/opd/ConsultationDesk";
import { FollowUpListPage } from "./features/opd/FollowUpListPage";
import { WalkinDesk } from "./features/opd/WalkinDesk";
import { DocumentUploader } from "./features/patient/DocumentUploader";
import { RegistrationForm } from "./features/patient/RegistrationForm";
import { PatientSearch } from "./features/patient/PatientSearch";
import { PatientProfile } from "./features/patient/PatientProfile";
import { useState } from "react";
import { BillListPage } from "./features/billing/BillListPage";
import { BillDetailPage } from "./features/billing/BillDetailPage";
import { NewBillPage } from "./features/billing/NewBillPage";
import { BillingDashboard } from "./features/billing/BillingDashboard";
import { PharmacyDashboard } from "./features/pharmacy/PharmacyDashboard";
import { NewSalePage } from "./features/pharmacy/NewSalePage";
import { SaleReturnsPage } from "./features/pharmacy/SaleReturnsPage";
import { DrugSafetyPage } from "./features/pharmacy/DrugSafetyPage";
import { NursingCarePage } from "./features/nursing/NursingCarePage";
import { NursingWorksListPage } from "./features/nursing/NursingWorksListPage";
import { LabOrdersPage } from "./features/lab/LabOrdersPage";
import { BedBoardPage } from "./features/adt/BedBoardPage";
import { QueueDashboardPage } from "./features/queue/QueueDashboardPage";
import { LoginPage } from "./features/auth/LoginPage";
import { AuthAdminPage } from "./features/auth/AuthAdminPage";
import { ReportsPage } from "./features/reports/ReportsPage";
import { ReportExecutionsPage } from "./features/reports/ReportExecutionsPage";
import { ReportDashboardPage } from "./features/reports/ReportDashboardPage";
import { PatientSubmissionsPage } from "./features/forms/SubmissionsPage";
import { InventoryPage } from "./features/inventory/InventoryPage";
import { OtPage } from "./features/ot/OtPage";
import { BloodBankPage } from "./features/bloodbank/BloodBankPage";
import { OrdersPage } from "./features/orders/OrdersPage";
import { ProcurementPage } from "./features/procurement/ProcurementPage";
import { DietPage } from "./features/diet/DietPage";
import { RadiologyPage } from "./features/radiology/RadiologyPage";
import { CssdPage } from "./features/cssd/CssdPage";
import { GrievancePage } from "./features/grievance/GrievancePage";
import { TransportPage } from "./features/transport/TransportPage";
import { TariffsPage } from "./features/tariffs/TariffsPage";
import { DoctorSharePage } from "./features/doctor-share/DoctorSharePage";
import { EquipmentPage } from "./features/equipment/EquipmentPage";
import { LinenFrontPage } from "./features/linen-f/LinenFrontPage";
import { RmrsPage } from "./features/rmrs/RmrsPage";
import { PmrdPage } from "./features/pmrd/PmrdPage";
import { WorklistsPage } from "./features/worklists/WorklistsPage";
import { MortuaryPage } from "./features/mortuary/MortuaryPage";
import { ChemotherapyPage } from "./features/chemotherapy/ChemotherapyPage";
import { KioskPage } from "./features/kiosk/KioskPage";
import { KioskSelfReg } from "./features/kiosk/KioskSelfReg";
import { KioskAppointment } from "./features/kiosk/KioskAppointment";
import { LabResultUpload } from "./features/kiosk/LabResultUpload";
import { PaymentTransferPage } from "./features/payment-transfer/PaymentTransferPage";
import { EjsPage } from "./features/ejs/EjsPage";
import { BmwPage } from "./features/bmw/BmwPage";
import { BcmaPage } from "./features/bcma/BcmaPage";
import { RcmNewPage } from "./features/rcm-new/RcmNewPage";
import { FilesPage } from "./features/files/FilesPage";
import { AasmaanPage } from "./features/aasmaan/AasmaanPage";
import { InteropNewPage } from "./features/interop-new/InteropNewPage";
import { AppConfigPage } from "./features/commons/AppConfig";
import { AuditTrailPage } from "./features/commons/AuditTrail";
import { HospitalSetupPage } from "./features/commons/HospitalSetup";

/**
 * App shell + routes.
 *   /masters/:entity        list (entity preselect; /masters and / also work)
 *   /masters/:entity/new    create mode
 *   /masters/:entity/:id    edit mode
 *
 *   /m/op/walkinVisit       walk-in registration (MIG-001 OP, 09 §2.1)
 *   /m/op/addAppointment   department -> doctor -> slot cascade
 *   /m/op/appointmentList  list with status / source badges + reschedule/cancel
 *   /m/op/abha             Aadhaar/Mobile OTP ABHA + QR scan + ABHA card
 *
 *   /m02/walkin            M02 Walk-in Desk (Patient search + walk-in check-in)
 *   /m02/booking           M02 Follow-up scheduler (Appointment Booking)
 *   /m02/consult/:id       M02 Doctor Consultation Desk (questionnaire / tele / OPD card)
 *   /m02/followups         M02 Follow-up visit list (by appointment / patient / doctor)
 *   /patients/register     M02 Patient Registration form
 *   /patients/:id/documents  M02 Document Uploader
 */
export default function App() {
  return (
    <AppShell>
      <Routes>
          <Route path="/" element={<MasterListPage />} />
          <Route path="/masters" element={<MasterListPage />} />
          <Route path="/masters/:entity" element={<MasterListPage />} />
          <Route path="/masters/:entity/new" element={<MasterEditPage />} />
          <Route path="/masters/:entity/:id" element={<MasterEditPage />} />
          <Route path="/forms" element={<FormListPage />} />
          <Route path="/forms/submissions" element={<PatientSubmissionsPage />} />
          <Route path="/m/op/walkinVisit" element={<WalkinVisitPage />} />
          <Route path="/m/op/addAppointment" element={<AddAppointmentPage />} />
          <Route path="/m/op/appointmentList" element={<AppointmentListPage />} />
          <Route path="/m/op/abha" element={<AbhaPage />} />
          <Route path="/m02/walkin" element={<WalkinDesk />} />
          <Route path="/m02/booking" element={<AppointmentBooking />} />
          <Route path="/m02/consult/:id" element={<ConsultationDeskRoute />} />
          <Route path="/m02/followups" element={<FollowUpListPage />} />
          <Route path="/patients/register" element={<RegistrationForm />} />
          <Route path="/patients/:id/documents" element={<DocumentsRoute />} />
          <Route path="/patients/:id" element={<ProfileRoute />} />
          <Route path="/patients" element={<PatientSearchDemo />} />
          <Route path="/billing" element={<BillListPage />} />
          <Route path="/billing/new" element={<NewBillPage />} />
          <Route path="/billing/:id" element={<BillDetailPage />} />
          <Route path="/billing/dashboard" element={<BillingDashboard />} />
          <Route path="/pharmacy" element={<PharmacyDashboard />} />
          <Route path="/pharmacy/new" element={<NewSalePage />} />
          <Route path="/pharmacy/returns" element={<SaleReturnsPage />} />
          <Route path="/pharmacy/safety" element={<DrugSafetyPage />} />
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/nursing" element={<NursingCarePage />} />
          <Route path="/nursing/worklist" element={<NursingWorksListPage />} />
          <Route path="/lab" element={<LabOrdersPage />} />
          <Route path="/adt" element={<BedBoardPage />} />
          <Route path="/queue" element={<QueueDashboardPage />} />
          <Route path="/auth/login" element={<LoginPage />} />
          <Route path="/auth/admin" element={<AuthAdminPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/reports/executions" element={<ReportExecutionsPage />} />
          <Route path="/reports/dashboard" element={<ReportDashboardPage />} />
          <Route path="/ot" element={<OtPage />} />
          <Route path="/bloodbank" element={<BloodBankPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/procurement" element={<ProcurementPage />} />
          <Route path="/diet" element={<DietPage />} />
          <Route path="/radiology" element={<RadiologyPage />} />
          <Route path="/cssd" element={<CssdPage />} />
          <Route path="/grievance" element={<GrievancePage />} />
          <Route path="/transport" element={<TransportPage />} />
          <Route path="/tariffs" element={<TariffsPage />} />
          <Route path="/doctor-share" element={<DoctorSharePage />} />
          <Route path="/equipment" element={<EquipmentPage />} />
          <Route path="/linen" element={<LinenFrontPage />} />
          <Route path="/rmrs" element={<RmrsPage />} />
          <Route path="/pmrd" element={<PmrdPage />} />
          <Route path="/worklists" element={<WorklistsPage />} />
          <Route path="/mortuary" element={<MortuaryPage />} />
          <Route path="/chemotherapy" element={<ChemotherapyPage />} />
          <Route path="/kiosk" element={<KioskPage />} />
          <Route path="/kiosk/self-register" element={<KioskSelfReg />} />
          <Route path="/kiosk/appointment" element={<KioskAppointment />} />
          <Route path="/kiosk/lab-result" element={<LabResultUpload />} />
          <Route path="/payment-transfer" element={<PaymentTransferPage />} />
          <Route path="/ejs" element={<EjsPage />} />
          <Route path="/bmw" element={<BmwPage />} />
          <Route path="/bcma" element={<BcmaPage />} />
          <Route path="/rcm" element={<RcmNewPage />} />
          <Route path="/files" element={<FilesPage />} />
          <Route path="/aasmaan" element={<AasmaanPage />} />
          <Route path="/interop" element={<InteropNewPage />} />
          <Route path="/commons/config" element={<AppConfigPage />} />
          <Route path="/commons/audit" element={<AuditTrailPage />} />
          <Route path="/commons/hospital" element={<HospitalSetupPage />} />
          <Route path="*" element={<MasterListPage />} />
      </Routes>
    </AppShell>
  );
}

// --- tiny inline route helpers so we don't need a separate file -------------

function ConsultationDeskRoute() {
  // useParams without importing the hook in two places
  // (the URL is /m02/consult/:id, appointmentId is required)
  // Reading the path directly avoids needing react-router imports here.
  const id = Number(window.location.pathname.split("/").pop());
  return <ConsultationDesk appointmentId={id || 0} />;
}

function DocumentsRoute() {
  const id = Number(window.location.pathname.split("/").slice(-2, -1)[0]);
  return <DocumentUploader patientId={id || 0} />;
}

function ProfileRoute() {
  const id = Number(window.location.pathname.split("/").slice(-2, -1)[0]);
  return (
    <div className="p-4">
      <PatientProfile patientId={id || 0} />
    </div>
  );
}

function PatientSearchDemo() {
  const [picked, setPicked] = useState<{ patientId: number } | null>(null);
  return (
    <div className="p-4 space-y-3">
      <PatientSearch onSelect={(p) => setPicked(p as { patientId: number })} />
      {picked && (
        <p className="text-sm text-green-600">
          Selected: {picked.patientId} —{" "}
          <Link to={`/patients/${picked.patientId}`} className="underline">view profile</Link>
          {" | "}
          <Link to={`/patients/${picked.patientId}/documents`} className="underline">upload documents</Link>
        </p>
      )}
    </div>
  );
}
