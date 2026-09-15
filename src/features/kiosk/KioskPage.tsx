import { useNavigate } from "react-router-dom";

export function KioskPage() {
  const navigate = useNavigate();

  return (
    <div className="kiosk-view">
      <div className="card kiosk-card">
        <h1 className="kiosk-title">Patient Kiosk</h1>
        <p className="kiosk-subtitle">Please select an option to continue</p>

        <div className="kiosk-grid">
          <button className="btn btn-primary kiosk-btn" onClick={() => navigate("/kiosk/self-register")}>
            Self Registration
          </button>
          <button className="btn btn-primary kiosk-btn" onClick={() => navigate("/kiosk/appointment")}>
            Book Appointment
          </button>
          <button className="btn btn-primary kiosk-btn" onClick={() => navigate("/kiosk/lab-result")}>
            Upload Lab Result
          </button>
        </div>
      </div>
    </div>
  );
}
