import { useState } from "react";
import { useUsers, useCreateUser, useRoles, useCreateRole, usePermissions, useCreatePermission, useNotificationTemplates, useCreateNotificationTemplate } from "./queries";
import { userSchema, roleSchema, permissionSchema, notifTemplateSchema, type UserValues, type RoleValues, type PermissionValues, type NotifTemplateValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function AuthAdminPage() {
  const [tab, setTab] = useState<"users" | "roles" | "permissions" | "templates">("users");

  return (
    <section className="page">
      <header className="page__header"><h1>IAM Administration</h1></header>
      <nav className="tabs">
        {(["users", "roles", "permissions", "templates"] as const).map((t) => (
          <button key={t} className={`btn ${tab === t ? "btn--primary" : ""}`} onClick={() => setTab(t)}>
            {t === "templates" ? "Notification Templates" : t.charAt(0).toUpperCase() + t.slice(1)}
          </button>
        ))}
      </nav>
      {tab === "users" && <UserTab />}
      {tab === "roles" && <RoleTab />}
      {tab === "permissions" && <PermissionTab />}
      {tab === "templates" && <TemplateTab />}
    </section>
  );
}

function UserTab() {
  const { data: users = [], isLoading } = useUsers();
  const createUser = useCreateUser();
  const form = useForm<UserValues>({ resolver: zodResolver(userSchema) });
  const [err, setErr] = useState<string | null>(null);

  const submit = async (v: UserValues) => {
    setErr(null);
    try {
      await createUser.mutateAsync(v as unknown as Record<string, unknown>);
      form.reset();
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  return (
    <>
      <h2>Users</h2>
      <form className="inline-form" onSubmit={form.handleSubmit(submit)}>
        <input placeholder="Username" {...form.register("username")} />
        <input type="password" placeholder="Password (min 8)" {...form.register("password")} />
        <input placeholder="Full name" {...form.register("fullName")} />
        <input placeholder="Email" {...form.register("email")} />
        <input type="number" placeholder="Employee ID" {...form.register("employeeId")} />
        <button type="submit" className="btn" disabled={createUser.isPending}>+ Create</button>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <table className="table">
          <thead><tr><th>ID</th><th>Username</th><th>Name</th><th>Email</th><th>Status</th><th>Roles</th></tr></thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td><td>{u.username}</td><td>{u.fullName ?? "—"}</td>
                <td>{u.email ?? "—"}</td>
                <td>{u.status ? "Active" : "Inactive"}</td>
                <td>{(u.roles ?? []).join(", ")}</td>
              </tr>
            ))}
            {users.length === 0 && <tr><td colSpan={6} className="muted">No users.</td></tr>}
          </tbody>
        </table>
      )}
    </>
  );
}

function RoleTab() {
  const { data: roles = [], isLoading } = useRoles();
  const createRole = useCreateRole();
  const form = useForm<RoleValues>({ resolver: zodResolver(roleSchema) });
  const [err, setErr] = useState<string | null>(null);

  const submit = async (v: RoleValues) => {
    setErr(null);
    try {
      await createRole.mutateAsync(v as unknown as Record<string, unknown>);
      form.reset();
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  return (
    <>
      <h2>Roles</h2>
      <form className="inline-form" onSubmit={form.handleSubmit(submit)}>
        <input placeholder="Role name" {...form.register("roleName")} />
        <input placeholder="Description" {...form.register("description")} />
        <button type="submit" className="btn" disabled={createRole.isPending}>+ Create</button>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <table className="table">
          <thead><tr><th>ID</th><th>Role</th><th>Description</th></tr></thead>
          <tbody>
            {roles.map((r) => (
              <tr key={r.roleId}><td>{r.roleId}</td><td>{r.roleName}</td><td>{r.description ?? "—"}</td></tr>
            ))}
            {roles.length === 0 && <tr><td colSpan={3} className="muted">No roles.</td></tr>}
          </tbody>
        </table>
      )}
    </>
  );
}

function PermissionTab() {
  const { data: perms = [], isLoading } = usePermissions();
  const createPerm = useCreatePermission();
  const form = useForm<PermissionValues>({ resolver: zodResolver(permissionSchema) });
  const [err, setErr] = useState<string | null>(null);

  const submit = async (v: PermissionValues) => {
    setErr(null);
    try {
      await createPerm.mutateAsync(v as unknown as Record<string, unknown>);
      form.reset();
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  return (
    <>
      <h2>Permissions</h2>
      <form className="inline-form" onSubmit={form.handleSubmit(submit)}>
        <input placeholder="Permission name" {...form.register("permissionName")} />
        <input placeholder="Module" {...form.register("module")} />
        <input placeholder="Description" {...form.register("description")} />
        <button type="submit" className="btn" disabled={createPerm.isPending}>+ Create</button>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <table className="table">
          <thead><tr><th>ID</th><th>Permission</th><th>Module</th></tr></thead>
          <tbody>
            {perms.map((p) => (
              <tr key={p.permissionId}><td>{p.permissionId}</td><td>{p.permissionName}</td><td>{p.module}</td></tr>
            ))}
            {perms.length === 0 && <tr><td colSpan={3} className="muted">No permissions.</td></tr>}
          </tbody>
        </table>
      )}
    </>
  );
}

function TemplateTab() {
  const { data: templates = [], isLoading } = useNotificationTemplates();
  const createTemplate = useCreateNotificationTemplate();
  const form = useForm<NotifTemplateValues>({ resolver: zodResolver(notifTemplateSchema) });
  const [err, setErr] = useState<string | null>(null);

  const submit = async (v: NotifTemplateValues) => {
    setErr(null);
    try {
      await createTemplate.mutateAsync(v as unknown as Record<string, unknown>);
      form.reset();
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  return (
    <>
      <h2>Notification Templates</h2>
      <form className="form" onSubmit={form.handleSubmit(submit)}>
        <fieldset>
          <legend>New Template</legend>
          <input placeholder="Template name" {...form.register("name")} />
          <input placeholder="Event type" {...form.register("eventType")} />
          <select {...form.register("channel")}>
            <option value="SMS">SMS</option>
            <option value="EMAIL">Email</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="PUSH">Push</option>
          </select>
          <textarea placeholder="Template body (use {placeholders})" {...form.register("template")} />
          <button type="submit" className="btn" disabled={createTemplate.isPending}>+ Create</button>
        </fieldset>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <table className="table">
          <thead><tr><th>Name</th><th>Event</th><th>Channel</th><th>Active</th></tr></thead>
          <tbody>
            {templates.map((t) => (
              <tr key={t.templateId}>
                <td>{t.name}</td><td>{t.eventType}</td><td>{t.channel}</td><td>{t.isActive ? "Yes" : "No"}</td>
              </tr>
            ))}
            {templates.length === 0 && <tr><td colSpan={4} className="muted">No templates.</td></tr>}
          </tbody>
        </table>
      )}
    </>
  );
}