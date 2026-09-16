import { useLabWorkload, useMachines } from './queries';

export function WorkloadDashboard() {
  const { data: workload = [], isLoading: workloadLoading } = useLabWorkload();
  const { data: machines = [], isLoading: machinesLoading } = useMachines();

  if (workloadLoading || machinesLoading) return <div className="p-4 text-center text-gray-500">Loading workload data...</div>;

  const totalPending = workload.reduce((sum, w) => sum + (w.pendingCount || 0), 0);
  const totalInProgress = workload.reduce((sum, w) => sum + (w.inProgressCount || 0), 0);
  const totalCompleted = workload.reduce((sum, w) => sum + (w.completedToday || 0), 0);

  return (
    <section className="space-y-6">
      <header><h2 className="text-xl font-semibold text-gray-900">Workload Dashboard</h2><p className="text-sm text-gray-500">Real-time machine queue status</p></header>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gradient-to-br from-yellow-50 to-yellow-100 border border-yellow-200 rounded-lg p-4"><div className="flex items-center justify-between"><div><p className="text-sm text-yellow-700 font-medium">Pending</p><p className="text-3xl font-bold text-yellow-900 mt-1">{totalPending}</p></div><div className="text-4xl opacity-50">⏳</div></div></div>
        <div className="bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 rounded-lg p-4"><div className="flex items-center justify-between"><div><p className="text-sm text-blue-700 font-medium">In Progress</p><p className="text-3xl font-bold text-blue-900 mt-1">{totalInProgress}</p></div><div className="text-4xl opacity-50">⚙️</div></div></div>
        <div className="bg-gradient-to-br from-green-50 to-green-100 border border-green-200 rounded-lg p-4"><div className="flex items-center justify-between"><div><p className="text-sm text-green-700 font-medium">Completed Today</p><p className="text-3xl font-bold text-green-900 mt-1">{totalCompleted}</p></div><div className="text-4xl opacity-50">✓</div></div></div>
      </div>
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <div className="bg-gray-50 px-4 py-3 border-b border-gray-200"><h3 className="text-sm font-medium text-gray-900">Machine Queue Status</h3></div>
        <div className="divide-y divide-gray-200">
          {machines.length === 0 ? (<div className="p-8 text-center text-gray-500">No machines configured</div>) : machines.map((machine) => {
            const mw = workload.find(w => w.machineId === machine.machineId);
            const pending = mw?.pendingCount || 0;
            const inProgress = mw?.inProgressCount || 0;
            const completed = mw?.completedToday || 0;
            const totalLoad = pending + inProgress;
            return (
              <div key={machine.machineId} className="p-4 hover:bg-gray-50">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3"><div className={`w-3 h-3 rounded-full ${machine.status === 'ACTIVE' ? 'bg-green-500' : machine.status === 'MAINTENANCE' ? 'bg-yellow-500' : 'bg-gray-400'}`} /><div><h4 className="text-sm font-medium text-gray-900">{machine.name}</h4><p className="text-xs text-gray-500 font-mono">{machine.code}</p></div></div>
                  <div className="flex items-center gap-4 text-sm"><div className="text-right"><div className="text-xs text-gray-500">Interface</div><div className="font-mono text-gray-700">{machine.interfaceType}</div></div><div className="text-right"><div className="text-xs text-gray-500">Status</div><div className={`font-medium ${machine.status === 'ACTIVE' ? 'text-green-700' : machine.status === 'MAINTENANCE' ? 'text-yellow-700' : 'text-gray-500'}`}>{machine.status}</div></div></div>
                </div>
                <div className="space-y-2"><div className="flex items-center justify-between text-xs text-gray-600"><span>Queue Load</span><span className="font-mono">{totalLoad} tests</span></div><div className="relative h-8 bg-gray-100 rounded overflow-hidden"><div className="absolute left-0 top-0 h-full bg-yellow-400 transition-all duration-300" style={{ width: `${totalLoad > 0 ? (pending / Math.max(totalLoad, 10)) * 100 : 0}%` }} /><div className="absolute top-0 h-full bg-blue-400 transition-all duration-300" style={{ left: `${totalLoad > 0 ? (pending / Math.max(totalLoad, 10)) * 100 : 0}%`, width: `${totalLoad > 0 ? (inProgress / Math.max(totalLoad, 10)) * 100 : 0}%` }} /><div className="absolute inset-0 flex items-center justify-center text-xs font-medium text-gray-700">{pending > 0 && <span className="mr-2">⏳ {pending}</span>}{inProgress > 0 && <span>⚙️ {inProgress}</span>}{totalLoad === 0 && <span className="text-gray-400">Idle</span>}</div></div><div className="flex items-center justify-between text-xs"><div className="flex items-center gap-4"><span className="flex items-center gap-1"><span className="w-3 h-3 bg-yellow-400 rounded" /><span className="text-gray-600">Pending ({pending})</span></span><span className="flex items-center gap-1"><span className="w-3 h-3 bg-blue-400 rounded" /><span className="text-gray-600">In Progress ({inProgress})</span></span></div><span className="text-green-700 font-medium">✓ {completed} completed</span></div></div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}