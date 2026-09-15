import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError } from "../../api/client";
import { mastersApi } from "../../api/client";
import { hospitalSetupSchema, type HospitalSetupValues } from "./zodSchema";

/**
 * HospitalSetup — geo-tagged unit (hospital) creation.
 *
 * Creates a UnitMaster record with geo-coordinates (longitude/latitude)
 * captured via browser Geolocation API or manual entry. On success the
 * backend emits an audit entry (entityType=UnitMaster, action=CREATE).
 *
 * Linear audit-log pattern: the create is recorded server-side; the
 * frontend only needs to show the result and any validation errors.
 */
export function HospitalSetupPage() {
  const [geoStatus, setGeoStatus] = useState<"idle" | "fetching" | "success" | "error">("idle");
  const [geoError, setGeoError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [created, setCreated] = useState<{ id: number; code: string; name: string } | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<HospitalSetupValues>({
    resolver: zodResolver(hospitalSetupSchema),
    defaultValues: {
      code: "",
      name: "",
      description: "",
      addressline1: "",
      city: "",
      state: "",
      country: "",
      pincode: "",
      contactno: "",
      email: "",
      longitude: "",
      latitude: "",
    },
  });

  const captureGeo = () => {
    if (!navigator.geolocation) {
      setGeoStatus("error");
      setGeoError("Geolocation is not supported by this browser.");
      return;
    }
    setGeoStatus("fetching");
    setGeoError(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setValue("longitude", String(pos.coords.longitude));
        setValue("latitude", String(pos.coords.latitude));
        setGeoStatus("success");
      },
      (err) => {
        setGeoStatus("error");
        setGeoError(err.message);
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  };

  const onSubmit = async (values: HospitalSetupValues) => {
    setSubmitting(true);
    setSubmitError(null);
    setCreated(null);
    try {
      const payload: Record<string, unknown> = {
        code: values.code,
        name: values.name,
      };
      if (values.description) payload.description = values.description;
      if (values.addressline1) payload.addressline1 = values.addressline1;
      if (values.city) payload.city = values.city;
      if (values.state) payload.state = values.state;
      if (values.country) payload.country = values.country;
      if (values.pincode) payload.pincode = values.pincode;
      if (values.contactno) payload.contactno = values.contactno;
      if (values.email) payload.email = values.email;
      if (values.longitude) payload.longitude = values.longitude;
      if (values.latitude) payload.latiutde = values.latitude;

      const result = await mastersApi.create("unitmaster", payload);
      setCreated({
        id: result.id as number,
        code: (result.code as string) ?? values.code,
        name: (result.name as string) ?? values.name,
      });
    } catch (err) {
      const msg =
        err instanceof ApiError
          ? `${err.code}: ${err.message}`
          : err instanceof Error
            ? err.message
            : "Failed to create unit";
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Hospital Setup</h1>
          <p className="muted">
            Create a new unit with geo-tagging · coordinates auto-captured or entered manually
          </p>
        </div>
      </header>

      {submitError ? (
        <div className="alert alert--error" role="alert">
          <strong>Creation failed</strong>: {submitError}
        </div>
      ) : null}

      {created ? (
        <div className="alert alert--success" role="status">
          <strong>Unit created</strong>: {created.name} ({created.code}) — ID {created.id}
        </div>
      ) : null}

      <section className="card">
        <form className="master-form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <h2>Basic Information</h2>
          <div className="form-grid">
            <div className={`field ${errors.code ? "field--error" : ""}`}>
              <label htmlFor="hs-code">
                Unit Code <span className="req" aria-hidden="true">*</span>
              </label>
              <input
                id="hs-code"
                type="text"
                placeholder="HOSP-001"
                aria-invalid={errors.code ? true : undefined}
                {...register("code")}
              />
              {errors.code ? (
                <p className="field-error" role="alert">{errors.code.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.name ? "field--error" : ""}`}>
              <label htmlFor="hs-name">
                Unit Name <span className="req" aria-hidden="true">*</span>
              </label>
              <input
                id="hs-name"
                type="text"
                placeholder="Jaipur Multispecialty Hospital"
                aria-invalid={errors.name ? true : undefined}
                {...register("name")}
              />
              {errors.name ? (
                <p className="field-error" role="alert">{errors.name.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.description ? "field--error" : ""}`}>
              <label htmlFor="hs-desc">Description</label>
              <input
                id="hs-desc"
                type="text"
                placeholder="200-bed tertiary care facility"
                aria-invalid={errors.description ? true : undefined}
                {...register("description")}
              />
              {errors.description ? (
                <p className="field-error" role="alert">{errors.description.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.contactno ? "field--error" : ""}`}>
              <label htmlFor="hs-phone">Contact No.</label>
              <input
                id="hs-phone"
                type="text"
                placeholder="+91-141-000-0000"
                aria-invalid={errors.contactno ? true : undefined}
                {...register("contactno")}
              />
              {errors.contactno ? (
                <p className="field-error" role="alert">{errors.contactno.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.email ? "field--error" : ""}`}>
              <label htmlFor="hs-email">Email</label>
              <input
                id="hs-email"
                type="text"
                placeholder="admin@hospital.org"
                aria-invalid={errors.email ? true : undefined}
                {...register("email")}
              />
              {errors.email ? (
                <p className="field-error" role="alert">{errors.email.message}</p>
              ) : null}
            </div>
          </div>

          <h2 style={{ marginTop: 24 }}>Address</h2>
          <div className="form-grid">
            <div className={`field ${errors.addressline1 ? "field--error" : ""}`}>
              <label htmlFor="hs-addr">Address Line</label>
              <input
                id="hs-addr"
                type="text"
                placeholder="123 MG Road"
                aria-invalid={errors.addressline1 ? true : undefined}
                {...register("addressline1")}
              />
              {errors.addressline1 ? (
                <p className="field-error" role="alert">{errors.addressline1.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.city ? "field--error" : ""}`}>
              <label htmlFor="hs-city">City</label>
              <input
                id="hs-city"
                type="text"
                placeholder="Jaipur"
                aria-invalid={errors.city ? true : undefined}
                {...register("city")}
              />
              {errors.city ? (
                <p className="field-error" role="alert">{errors.city.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.state ? "field--error" : ""}`}>
              <label htmlFor="hs-state">State</label>
              <input
                id="hs-state"
                type="text"
                placeholder="Rajasthan"
                aria-invalid={errors.state ? true : undefined}
                {...register("state")}
              />
              {errors.state ? (
                <p className="field-error" role="alert">{errors.state.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.country ? "field--error" : ""}`}>
              <label htmlFor="hs-country">Country</label>
              <input
                id="hs-country"
                type="text"
                placeholder="India"
                aria-invalid={errors.country ? true : undefined}
                {...register("country")}
              />
              {errors.country ? (
                <p className="field-error" role="alert">{errors.country.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.pincode ? "field--error" : ""}`}>
              <label htmlFor="hs-pin">PIN Code</label>
              <input
                id="hs-pin"
                type="text"
                placeholder="302001"
                aria-invalid={errors.pincode ? true : undefined}
                {...register("pincode")}
              />
              {errors.pincode ? (
                <p className="field-error" role="alert">{errors.pincode.message}</p>
              ) : null}
            </div>
          </div>

          <h2 style={{ marginTop: 24 }}>
            Geo-Tagging
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={captureGeo}
              disabled={geoStatus === "fetching"}
              style={{ marginLeft: 12 }}
            >
              {geoStatus === "fetching" ? "Capturing…" : "📍 Auto-capture"}
            </button>
          </h2>
          {geoStatus === "success" ? (
            <p className="notice" role="status" style={{ color: "var(--color-success)" }}>
              ✓ Coordinates captured
            </p>
          ) : null}
          {geoStatus === "error" ? (
            <p className="field-error" role="alert">{geoError}</p>
          ) : null}

          <div className="form-grid">
            <div className={`field ${errors.longitude ? "field--error" : ""}`}>
              <label htmlFor="hs-lon">Longitude</label>
              <input
                id="hs-lon"
                type="text"
                placeholder="75.7873"
                aria-invalid={errors.longitude ? true : undefined}
                {...register("longitude")}
              />
              {errors.longitude ? (
                <p className="field-error" role="alert">{errors.longitude.message}</p>
              ) : null}
            </div>

            <div className={`field ${errors.latitude ? "field--error" : ""}`}>
              <label htmlFor="hs-lat">Latitude</label>
              <input
                id="hs-lat"
                type="text"
                placeholder="26.9124"
                aria-invalid={errors.latitude ? true : undefined}
                {...register("latitude")}
              />
              {errors.latitude ? (
                <p className="field-error" role="alert">{errors.latitude.message}</p>
              ) : null}
            </div>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Creating…" : "Create Unit"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
