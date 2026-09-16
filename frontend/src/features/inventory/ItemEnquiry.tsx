import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Checkbox, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { itemEnquiryFilterSchema, type ItemEnquiryFilterValues } from "./zodSchema";
import { useItemEnquiry } from "./queries";

export function ItemEnquiry() {
  const { register, watch, setValue } = useForm<ItemEnquiryFilterValues>({
    resolver: zodResolver(itemEnquiryFilterSchema),
    defaultValues: {
      keyword: "",
      storeId: undefined,
      category: "",
      lowStockOnly: false,
      status: "",
    },
  });

  const filters = watch();
  const lowStockOnly = watch("lowStockOnly");
  const { data: items = [], isLoading, isFetching } = useItemEnquiry({
    keyword: filters.keyword || undefined,
    storeId: filters.storeId || undefined,
    category: filters.category || undefined,
    lowStockOnly: filters.lowStockOnly || undefined,
    status: filters.status || undefined,
  });

  const categories = useMemo(
    () => Array.from(new Set(items.map((i) => i.category).filter(Boolean))) as string[],
    [items],
  );

  return (
    <div className="item-enquiry">
      <form onSubmit={(e) => e.preventDefault()} className="item-enquiry__filters">
        <SimpleGrid columns={{ base: 1, md: 4 }} gap={4}>
          <FormField label="Search">
            <TextInput
              id="enq-keyword"
              aria-label="Search"
              type="text"
              placeholder="Item name or code..."
              {...register("keyword")}
            />
          </FormField>

          <FormField label="Category">
            <HimsSelect id="enq-category" aria-label="Category" {...register("category")}>
              <option value="">All categories</option>
              {categories.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </HimsSelect>
          </FormField>

          <FormField label="Status">
            <HimsSelect id="enq-status" aria-label="Status" {...register("status")}>
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="DISCONTINUED">Discontinued</option>
            </HimsSelect>
          </FormField>

          <Checkbox.Root
            checked={!!lowStockOnly}
            onCheckedChange={(e) => setValue("lowStockOnly", !!e.checked, { shouldValidate: true })}
          >
            <Checkbox.HiddenInput id="enq-low-stock" aria-label="Low stock only" {...register("lowStockOnly")} />
            <Checkbox.Control />
            <Checkbox.Label>Low stock only</Checkbox.Label>
          </Checkbox.Root>
        </SimpleGrid>
      </form>

      {isLoading ? (
        <div className="loading-state">
          <span className="spinner" />
          Loading items...
        </div>
      ) : (
        <>
          <div className="item-enquiry__results-info">
            <span className="muted">
              {items.length} item{items.length !== 1 ? "s" : ""} found
              {isFetching && " (refreshing...)"}
            </span>
          </div>

          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Category</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Current Stock</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reorder Lvl</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Max Stock</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Batches</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {items.map((item) => (
                <Table.Row key={item.id}>
                  <Table.Cell>{item.name}</Table.Cell>
                  <Table.Cell>{item.code}</Table.Cell>
                  <Table.Cell>{item.category ?? "—"}</Table.Cell>
                  <Table.Cell>
                    <span className={item.currentStock <= item.reorderLevel ? "text--warning" : ""}>
                      {item.currentStock}
                    </span>
                  </Table.Cell>
                  <Table.Cell>{item.reorderLevel}</Table.Cell>
                  <Table.Cell>{item.maxStock}</Table.Cell>
                  <Table.Cell>
                    {item.batches.length > 0 ? (
                      <details className="batch-details">
                        <summary>{item.batches.length} batch{item.batches.length !== 1 ? "es" : ""}</summary>
                        <Table.Root size="sm" variant="line">
                          <Table.Header>
                            <Table.Row>
                              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Batch</Table.ColumnHeader>
                              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Expiry</Table.ColumnHeader>
                              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty</Table.ColumnHeader>
                            </Table.Row>
                          </Table.Header>
                          <Table.Body>
                            {item.batches.map((b) => (
                              <Table.Row key={b.id}>
                                <Table.Cell>{b.batchNumber}</Table.Cell>
                                <Table.Cell>{b.expiryDate}</Table.Cell>
                                <Table.Cell>{b.quantity}</Table.Cell>
                              </Table.Row>
                            ))}
                          </Table.Body>
                        </Table.Root>
                      </details>
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </Table.Cell>
                  <Table.Cell>
                    <span className={`badge badge--${item.status === "ACTIVE" ? "success" : item.status === "INACTIVE" ? "muted" : "error"}`}>
                      {item.status}
                    </span>
                  </Table.Cell>
                </Table.Row>
              ))}
              {items.length === 0 && (
                <Table.Row>
                  <Table.Cell colSpan={8}>No items match your filters.</Table.Cell>
                </Table.Row>
              )}
            </Table.Body>
          </Table.Root>
        </>
      )}
    </div>
  );
}
