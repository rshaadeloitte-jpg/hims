import { useState, useEffect } from "react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import { useVitals, useNursingTasks } from "./queries";
import { vitalsSchema, nursingTaskSchema } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";

export function NursingWorklist() {
  const [patientId, setPatientId] = useState(0);
  const [admissionId, setAdmissionId] = useState(0);
  const { data: vitals = [] } = useVitals(patientId);
  const { data: tasks = [] } = useNursingTasks();
  
  // Transform vitals data for charting (Epic MyChart style)
  const chartData = vitals.slice(-20).map(v => ({
    time: new Date(v.recordedAt).toLocaleTimeString(),
    temperature: v.temperature ?? null,
    pulse: v.pulse ?? null,
    "Blood Pressure": v.bloodPressureSystolic && v.bloodPressureDiastolic 
      ? `${v.bloodPressureSystolic}/${v.bloodPressureDiastolic}` 
      : null,
    "SpO₂": v.spO2 ?? null
  }));

  // Organize tasks by status (Linear-style Kanban)
  const taskColumns = {
    "To Do": tasks.filter(t => !t.completed && !t.cancelled),
    "In Progress": tasks.filter(t => t.assignedTo && !t.completed && !t.cancelled),
    "Completed": tasks.filter(t => t.completed),
    "Cancelled": tasks.filter(t => t.cancelled)
  };

  return (
    <div className="space-y-6 p-4">
      <header className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Nursing Worklist</h1>
        <div className="flex gap-4">
          <input 
            type="number" 
            placeholder="Patient ID" 
            value={patientId || ""} 
            onChange={(e) => setPatientId(Number(e.target.value))} 
            className="border rounded px-2 py-1"
          />
          <input 
            type="number" 
            placeholder="Admission ID" 
            value={admissionId || ""} 
            onChange={(e) => setAdmissionId(Number(e.target.value))} 
            className="border rounded px-2 py-1"
          />
        </div>
      </header>

      {patientId > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Vitals Trend (Last 20)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis yAxisId="left" />
                  <YAxis yAxisId="right" orientation="right" />
                  <Tooltip />
                  <Legend />
                  <Line yAxisId="left" type="monotone" dataKey="temperature" stroke="#8884d8" name="Temp (°C)" />
                  <Line yAxisId="left" type="monotone" dataKey="pulse" stroke="#82ca9d" name="Pulse (bpm)" />
                  <Line yAxisId="right" type="monotone" dataKey="Blood Pressure" stroke="#ffc658" name="BP (mmHg)" />
                  <Line yAxisId="left" type="monotone" dataKey="SpO₂" stroke="#ff7300" name="SpO₂ (%)" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="flex gap-4 overflow-x-auto pb-4">
        {Object.entries(taskColumns).map(([status, columnTasks]) => (
          <div key={status} className="flex-shrink-0 w-80">
            <Card>
              <CardHeader className="bg-muted">
                <CardTitle className="text-sm font-semibold">{status} ({columnTasks.length})</CardTitle>
              </CardHeader>
              <CardContent>
                <ScrollArea className="h-[400px]">
                  <div className="space-y-2">
                    {columnTasks.map(task => (
                      <div 
                        key={task.taskId} 
                        className="border rounded p-3 bg-card hover:shadow transition-shadow"
                      >
                        <p className="font-medium">{task.taskType}</p>
                        <p className="text-sm text-muted-foreground line-clamp-2">{task.description}</p>
                        <div className="mt-2 flex justify-between text-xs">
                          <span>Patient: {task.patientId}</span>
                          <span>Admission: {task.admissionId}</span>
                        </div>
                      </div>
                    ))}
                    {columnTasks.length === 0 && (
                      <p className="text-center text-muted-foreground py-4">No tasks</p>
                    )}
                  </div>
                </ScrollArea>
              </CardContent>
            </Card>
          </div>
        ))}
      </div>
    </div>
  );
}