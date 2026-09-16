import { useState } from "react";
import { Link } from "react-router-dom";
import { useBills, type BillFilter } from "./queries";
import { billFilterSchema, type BillFilterValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Button, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";

const statusBadge = (label: string, tone: "green" | "red" | "yellow" | "gray") => (
  <span className={`badge badge--${tone}`}>{label}</span>
);

const billStatus = (b: { isSettled: boolean; isCancelled: boolean; isInterim: boolean }) => {
  if (b.isCancelled) return statusBadge("Cancelled", "red");
  if (b.isSettled) return statusBadge("Settled", "green");
  if (b.isInterim) return statusBadge("Interim", "yellow");
  return statusBadge("Open", "gray");
};

export function BillListPage() {
  const { register, handleSubmit } = useForm<BillFilterValues>({
    resolver: zodResolver(billFilterSchema),
    defaultValues: { page: 0, size: 20 },
  });
  const [filter, setFilter] = useState<BillFilter>({ page: 0, size: 20 });
  const { data: bills = [], isLoading, isError, error } = useBills(filter);

  const onSubmit = (values: BillFilterValues) => {
    setFilter({
      patientId: values.patientId,
      fromDate: values.fromDate,
      toDate: values.toDate,
      isSettled: values.isSettled === "" ? undefined : values.isSettled === "true",
      page: values.page,
      size: values.size,
    });
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>Bills</h1>
        <Link to="/billing/new" className="btn btn--primary">+ New Bill</Link>
      </header>

      <form className="filters" onSubmit={handleSubmit(onSubmit)}>
        <FormField label="Patient ID">
          <TextInput type="number" {...register("patientId")} />
        </FormField>
        <FormField label="From">
          <TextInput type="date" {...register("fromDate")} />
        </FormField>
        <FormField label="To">
          <TextInput type="date" {...register("toDate")} />
        </FormField>
        <FormField label="Status">
          <HimsSelect {...register("isSettled")}>
            <option value="">All</option>
            <option value="true">Settled</option>
            <option value="false">Unsettled</option>
          </HimsSelect>
        </FormField>
        <Button type="submit" variant="primary">Search</Button>
      </form>

      {isLoading && <p>Loading…</p>}
      {isError && <p className="error">{(error as Error).message}</p>}

      <Table.Root size="sm" variant="line">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Bill No</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Total</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Balance</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {bills.map((b) => (
            <Table.Row key={b.billId}>
              <Table.Cell><Link to={`/billing/${b.billId}`}>{b.billNo}</Link></Table.Cell>
              <Table.Cell>{b.patientId}</Table.Cell>
              <Table.Cell>{b.totalAmount?.toFixed(2)}</Table.Cell>
              <Table.Cell>{(b.balanceSelf + b.balanceNonSelf).toFixed(2)}</Table.Cell>
              <Table.Cell>{billStatus(b)}</Table.Cell>
              <Table.Cell>{b.finalizedAt?.slice(0, 10) ?? "—"}</Table.Cell>
            </Table.Row>
          ))}
          {bills.length === 0 && !isLoading && (
            <Table.Row><Table.Cell colSpan={6} className="muted">No bills found.</Table.Cell></Table.Row>
          )}
        </Table.Body>
      </Table.Root>
    </section>
  );
}
