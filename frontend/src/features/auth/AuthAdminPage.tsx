import { useState } from "react";
import { useUsers, useCreateUser, useRoles, useCreateRole, usePermissions, useCreatePermission, useNotificationTemplates, useCreateNotificationTemplate } from "./queries";
import { userSchema, roleSchema, permissionSchema, notifTemplateSchema, type UserValues, type RoleValues, type PermissionValues, type NotifTemplateValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { HimsSelect, TextInput } from "../../components/ui";
import { Button, Table, Textarea } from "@chakra-ui/react";

export function AuthAdminPage() {
  const [tab, setTab] = useState<"users" | "roles" | "permissions" | "templates">("users");

  return (
    <section className="page">
      <header className="page__header"><h1>IAM Administration</h1></header>
      <nav className="tabs">
        {(["users", "roles", "permissions", "templates"] as const).map((t) => (
          <Button
            variant={tab === t ? "solid" : "outline"}
            size="sm"
            key={t}
            onClick={() => setTab(t)}
          >
            {t === "templates" ? "Notification Templates" : t.charAt(0).toUpperCase() + t.slice(1)}
          </Button>
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
        <TextInput placeholder="Username" {...form.register("username")} />
        <TextInput type="password" placeholder="Password (min 8)" {...form.register("password")} />
        <TextInput placeholder="Full name" {...form.register("fullName")} />
        <TextInput placeholder="Email" {...form.register("email")} />
        <TextInput type="number" placeholder="Employee ID" {...form.register("employeeId")} />
        <Button variant="primary" loading={createUser.isPending} type="submit" disabled={createUser.isPending}>+ Create</Button>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Username</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Email</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Roles</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {users.map((u) => (
              <Table.Row key={u.id ?? u.userId ?? u.username}>
                <Table.Cell>{u.id ?? u.userId}</Table.Cell><Table.Cell>{u.username}</Table.Cell><Table.Cell>{u.fullName ?? "—"}</Table.Cell>
                <Table.Cell>{u.email ?? "—"}</Table.Cell>
                <Table.Cell>{typeof u.status === "string" ? u.status : (u.status ?? u.isActive) ? "Active" : "Inactive"}</Table.Cell>
                <Table.Cell>{(u.roles ?? []).join(", ")}</Table.Cell>
              </Table.Row>
            ))}
            {users.length === 0 && <Table.Row><Table.Cell colSpan={6}>No users.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
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
        <TextInput placeholder="Role name" {...form.register("roleName")} />
        <TextInput placeholder="Description" {...form.register("description")} />
        <Button variant="primary" loading={createRole.isPending} type="submit" disabled={createRole.isPending}>+ Create</Button>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Role</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Description</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {roles.map((r) => (
              <Table.Row key={r.roleId}><Table.Cell>{r.roleId}</Table.Cell><Table.Cell>{r.roleName}</Table.Cell><Table.Cell>{r.description ?? "—"}</Table.Cell></Table.Row>
            ))}
            {roles.length === 0 && <Table.Row><Table.Cell colSpan={3}>No roles.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
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
        <TextInput placeholder="Permission name" {...form.register("permissionName")} />
        <TextInput placeholder="Module" {...form.register("module")} />
        <TextInput placeholder="Description" {...form.register("description")} />
        <Button variant="primary" loading={createPerm.isPending} type="submit" disabled={createPerm.isPending}>+ Create</Button>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Permission</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Module</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {perms.map((p) => (
              <Table.Row key={p.permissionId}><Table.Cell>{p.permissionId}</Table.Cell><Table.Cell>{p.permissionName}</Table.Cell><Table.Cell>{p.module}</Table.Cell></Table.Row>
            ))}
            {perms.length === 0 && <Table.Row><Table.Cell colSpan={3}>No permissions.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
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
          <TextInput placeholder="Template name" {...form.register("name")} />
          <TextInput placeholder="Event type" {...form.register("eventType")} />
          <HimsSelect {...form.register("channel")}>
            <option value="SMS">SMS</option>
            <option value="EMAIL">Email</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="PUSH">Push</option>
          </HimsSelect>
          <Textarea bg="white" borderColor="gray.300" placeholder="Template body (use {placeholders})" {...form.register("template")} />
          <Button variant="primary" loading={createTemplate.isPending} type="submit" disabled={createTemplate.isPending}>+ Create</Button>
        </fieldset>
      </form>
      {err && <p className="error">{err}</p>}
      {isLoading ? <p>Loading…</p> : (
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Event</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Channel</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Active</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {templates.map((t) => (
              <Table.Row key={t.templateId}>
                <Table.Cell>{t.name ?? t.templateCode}</Table.Cell><Table.Cell>{t.eventType ?? t.channel}</Table.Cell><Table.Cell>{t.channel}</Table.Cell><Table.Cell>{t.isActive ? "Yes" : "No"}</Table.Cell>
              </Table.Row>
            ))}
            {templates.length === 0 && <Table.Row><Table.Cell colSpan={4}>No templates.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      )}
    </>
  );
}