import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  questionnaireSchema,
  type QuestionnaireValues,
} from "./zodSchema";
import {
  useCreateTelemedicine,
  useEndTelemedicine,
  useGetOpdCard,
  useGetOpdCardHtml,
  useGetQuestionnaire,
  useGetTelemedicine,
  useStartTelemedicine,
  useSubmitQuestionnaire,
} from "./queries";

/**
 * M02 Doctor Consultation Desk (G-1). Tabbed surface covering:
 *  - Pre-consult questionnaire
 *  - Telemedicine room (create / start / end)
 *  - OPD card (data + printable HTML)
 *  - Follow-up scheduling (delegated to AppointmentBooking)
 */
export function ConsultationDesk({ appointmentId }: { appointmentId: number }) {
  const [tab, setTab] = useState<"questionnaire" | "telemedicine" | "opdcard" | "followup">("questionnaire");

  return (
    <div className="p-4 border rounded space-y-4" aria-label="consultation-desk">
      <h2 className="text-xl font-semibold">Consultation Desk — Appt #{appointmentId}</h2>
      <div className="flex gap-2 border-b">
        {(["questionnaire", "telemedicine", "opdcard", "followup"] as const).map((t) => (
          <button
            key={t}
            className={`px-3 py-2 capitalize ${
              tab === t ? "border-b-2 border-blue-600 font-medium" : "text-gray-500"
            }`}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === "questionnaire" && <QuestionnaireTab appointmentId={appointmentId} />}
      {tab === "telemedicine" && <TelemedicineTab appointmentId={appointmentId} />}
      {tab === "opdcard" && <OpdCardTab appointmentId={appointmentId} />}
      {tab === "followup" && (
        <div className="text-sm text-gray-600">
          Use the <strong>Appointment Booking</strong> page (it owns the follow-up form).
        </div>
      )}
    </div>
  );
}

function QuestionnaireTab({ appointmentId }: { appointmentId: number }) {
  const existing = useGetQuestionnaire(appointmentId);
  const submit = useSubmitQuestionnaire();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<QuestionnaireValues>({
    resolver: zodResolver(questionnaireSchema),
    defaultValues: { module: "Patient Assessment", moduleType: "", answers: [] },
  });

  const onSubmit = handleSubmit(async (values) => {
    await submit.mutateAsync({
      id: appointmentId,
      payload: {
        module: values.module,
        moduleType: values.moduleType || undefined,
        answers: JSON.stringify(values.answers ?? []),
        clinicianReviewFlag: false,
      },
    });
    reset({ module: values.module, moduleType: "", answers: [] });
  });

  return (
    <form onSubmit={onSubmit} className="space-y-2" aria-label="questionnaire-form">
      {existing.data && (
        <p className="text-xs text-gray-500">Last submitted: {new Date(existing.data.id).toLocaleString()}</p>
      )}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
        <label className="text-sm">
          <span className="block font-medium">Module</span>
          <select className="w-full border rounded px-2 py-1" {...register("module")}>
            <option>Patient Assessment</option>
            <option>Doctor Assessment</option>
            <option>Social Assessment</option>
          </select>
        </label>
        <label className="text-sm">
          <span className="block font-medium">Module type</span>
          <input className="w-full border rounded px-2 py-1" {...register("moduleType")} />
        </label>
      </div>
      {errors.module && <p className="text-red-600 text-xs">{errors.module.message}</p>}

      {submit.isError && <p className="text-sm text-red-600">{(submit.error as Error).message}</p>}
      {submit.isSuccess && <p className="text-sm text-green-600">Submitted. ID: {submit.data.id}</p>}

      <button
        type="submit"
        className="bg-green-600 text-white px-4 py-2 rounded disabled:opacity-50"
        disabled={isSubmitting || submit.isPending}
      >
        {submit.isPending ? "Submitting..." : "Submit Questionnaire"}
      </button>
    </form>
  );
}

function TelemedicineTab({ appointmentId }: { appointmentId: number }) {
  const room = useGetTelemedicine(appointmentId);
  const create = useCreateTelemedicine();
  const start = useStartTelemedicine();
  const end = useEndTelemedicine();

  return (
    <div className="space-y-3" aria-label="telemedicine-tab">
      {room.data && (
        <div className="p-3 bg-gray-50 border rounded text-sm space-y-1">
          <div>Room: <code>{room.data.roomId}</code></div>
          <div>Status: <code>{room.data.roomStatus}</code></div>
          {room.data.videoLink && (
            <div>
              Link: <a className="text-blue-600 underline" href={room.data.videoLink}>{room.data.videoLink}</a>
            </div>
          )}
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          className="bg-purple-600 text-white px-3 py-2 rounded disabled:opacity-50"
          onClick={() => create.mutate(appointmentId)}
          disabled={create.isPending || !!room.data}
        >
          {create.isPending ? "Creating..." : room.data ? "Room exists" : "Create Room"}
        </button>
        {room.data && (() => {
          const r = room.data;
          return (
            <>
              <button
                type="button"
                className="bg-blue-600 text-white px-3 py-2 rounded disabled:opacity-50"
                onClick={() => start.mutate(r.id)}
                disabled={start.isPending || r.roomStatus === 2}
              >
                {start.isPending ? "Starting..." : "Start Session"}
              </button>
              <button
                type="button"
                className="bg-red-600 text-white px-3 py-2 rounded disabled:opacity-50"
                onClick={() => end.mutate(r.id)}
                disabled={end.isPending || r.roomStatus !== 2}
              >
                {end.isPending ? "Ending..." : "End Session"}
              </button>
            </>
          );
        })()}
      </div>

      {(create.error || start.error || end.error) && (
        <p className="text-sm text-red-600">
          {(create.error || start.error || end.error) instanceof Error
            ? ((create.error || start.error || end.error) as Error).message
            : "Action failed"}
        </p>
      )}
    </div>
  );
}

function OpdCardTab({ appointmentId }: { appointmentId: number }) {
  const card = useGetOpdCard(appointmentId);
  const html = useGetOpdCardHtml(appointmentId);
  const [printable, setPrintable] = useState(false);

  return (
    <div className="space-y-3" aria-label="opd-card-tab">
      <div className="flex gap-2">
        <button
          type="button"
          className={`px-3 py-1 rounded ${!printable ? "bg-blue-600 text-white" : "bg-gray-200"}`}
          onClick={() => setPrintable(false)}
        >
          Data
        </button>
        <button
          type="button"
          className={`px-3 py-1 rounded ${printable ? "bg-blue-600 text-white" : "bg-gray-200"}`}
          onClick={() => setPrintable(true)}
        >
          Print HTML
        </button>
        <button type="button" className="px-3 py-1 rounded bg-gray-700 text-white" onClick={() => window.print()}>
          Print
        </button>
      </div>

      {!printable && (
        <div className="border rounded p-3 text-sm space-y-1">
          {card.isLoading && <p className="text-gray-500">Loading card...</p>}
          {card.data &&
            Object.entries(card.data).map(([k, v]) => (
              <div key={k} className="grid grid-cols-3 gap-2">
                <span className="text-gray-500">{k}</span>
                <span className="col-span-2 break-words">{String(v)}</span>
              </div>
            ))}
        </div>
      )}

      {printable && (
        <div className="border rounded p-3">
          {html.isLoading && <p className="text-gray-500">Loading HTML...</p>}
          {html.data && <div dangerouslySetInnerHTML={{ __html: html.data }} />}
        </div>
      )}
    </div>
  );
}
