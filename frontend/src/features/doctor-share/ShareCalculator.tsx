import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useDoctorShareCalculator, useCreateDoctorShare } from "./queries";
import { doctorShareFormSchema, type DoctorShareFormValues } from "./zodSchema";

interface ShareCalculatorProps {
  initialDoctorId?: number;
  initialService?: string;
  initialBaseAmount?: number;
  onCalculated?: (result: {
    baseAmount: number;
    sharePercent: number;
    shareAmount: number;
    gstAmount: number;
    netAmount: number;
  }) => void;
}

export function ShareCalculator({
  initialDoctorId = 0,
  initialService = "",
  initialBaseAmount = 0,
  onCalculated,
}: ShareCalculatorProps) {
  const [doctorId, setDoctorId] = useState(initialDoctorId);
  const [service, setService] = useState(initialService);
  const [baseAmount, setBaseAmount] = useState(initialBaseAmount);

  const { mutate: createShare, isPending: isCreating } = useCreateDoctorShare();

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
  } = useForm<DoctorShareFormValues>({
    resolver: zodResolver(doctorShareFormSchema),
    defaultValues: {
      doctorId: initialDoctorId,
      service: initialService,
      sharePercent: 0,
      baseAmount: initialBaseAmount,
      period: "MONTHLY",
    },
  });

  const { data: calcResult, isLoading: isCalculating } = useDoctorShareCalculator({
    doctorId,
    service,
    baseAmount,
  });

  const onSubmit = (values: DoctorShareFormValues) => {
    createShare(values, {
      onSuccess: (data) => {
        onCalculated?.({
          baseAmount: data.baseAmount ?? values.baseAmount,
          sharePercent: data.sharePercent ?? values.sharePercent,
          shareAmount: (data.baseAmount ?? values.baseAmount) * (data.sharePercent ?? values.sharePercent) / 100,
          gstAmount: 0,
          netAmount: (data.baseAmount ?? values.baseAmount) * (data.sharePercent ?? values.sharePercent) / 100,
        });
        reset();
      },
    });
  };

  const handleAdd = () => {
    const percent = watch("sharePercent");
    const amount = watch("baseAmount");
    if (percent && amount) {
      const share = (amount * percent) / 100;
      onCalculated?.({
        baseAmount: amount,
        sharePercent: percent,
        shareAmount: Math.round(share),
        gstAmount: Math.round(share * 0.05),
        netAmount: Math.round(share * 1.05),
      });
    }
  };

  const handleDeduct = () => {
    const percent = watch("sharePercent");
    const amount = watch("baseAmount");
    if (percent && amount) {
      const share = (amount * percent) / 100;
      const currentBase = watch("baseAmount");
      const newBase = Math.max(0, currentBase - Math.round(share * 0.05));
      setBaseAmount(newBase);
    }
  };

  return (
    <section className="page">
      <header className="page-header">
        <h1>Doctor Share Calculator</h1>
      </header>
      <div className="card">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="form-group">
            <label htmlFor="doctorId">Doctor ID</label>
            <input
              id="doctorId"
              type="number"
              {...register("doctorId")}
              onChange={(e) => setDoctorId(Number(e.target.value))}
              className="input"
              min={1}
            />
            {errors.doctorId && <span className="error">{errors.doctorId.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="service">Service</label>
            <input
              id="service"
              type="text"
              {...register("service")}
              onChange={(e) => setService(e.target.value)}
              className="input"
              placeholder="Enter service name"
            />
            {errors.service && <span className="error">{errors.service.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="baseAmount">Base Amount (₹)</label>
            <input
              id="baseAmount"
              type="number"
              {...register("baseAmount")}
              onChange={(e) => setBaseAmount(Number(e.target.value))}
              className="input"
              min={0}
              placeholder="0"
            />
            {errors.baseAmount && <span className="error">{errors.baseAmount.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="sharePercent">Share Percent (%)</label>
            <input
              id="sharePercent"
              type="number"
              {...register("sharePercent")}
              onChange={(e) => setDoctorId(Number(e.target.value))}
              className="input"
              min={1}
              max={100}
              placeholder="1-100"
            />
            {errors.sharePercent && <span className="error">{errors.sharePercent.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="period">Period</label>
            <select {...register("period")} className="input">
              <option value="WEEKLY">Weekly</option>
              <option value="MONTHLY">Monthly</option>
              <option value="QUARTERLY">Quarterly</option>
              <option value="YEARLY">Yearly</option>
            </select>
          </div>

          <div className="form-actions">
            <button type="button" onClick={handleAdd} className="btn btn-primary" disabled={isCalculating}>
              Add Share
            </button>
            <button type="button" onClick={handleDeduct} className="btn btn-secondary">
              Deduct
            </button>
            <button type="submit" className="btn btn-accent" disabled={isCreating}>
              {isCreating ? "Saving..." : "Save"}
            </button>
          </div>
        </form>
      </div>

      {calcResult && (
        <div className="card mt-4">
          <h2>Calculation Result</h2>
          <table className="table">
            <tbody>
              <tr><td className="muted">Base Amount</td><td>₹{calcResult.baseAmount.toLocaleString()}</td></tr>
              <tr><td className="muted">Share %</td><td>{calcResult.sharePercent}%</td></tr>
              <tr><td className="muted">Share Amount</td><td>₹{calcResult.shareAmount.toLocaleString()}</td></tr>
              <tr><td className="muted">GST (5%)</td><td>₹{calcResult.gstAmount.toLocaleString()}</td></tr>
              <tr><td className="muted font-semibold">Net Amount</td><td>₹{calcResult.netAmount.toLocaleString()}</td></tr>
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}