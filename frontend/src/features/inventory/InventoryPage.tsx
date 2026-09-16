import { useState } from "react";
import { Button, HStack } from "@chakra-ui/react";
import { ItemsTab, StoresTab, StockTab } from "./components/InventoryTabs";

const TABS = [
  { id: "items", label: "Items" },
  { id: "stores", label: "Stores" },
  { id: "stock", label: "Stock" },
  { id: "movements", label: "Movements" },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function InventoryPage() {
  const [activeTab, setActiveTab] = useState<TabId>("items");

  return (
    <div className="page">
      <header className="page__header">
        <h1>Inventory Management</h1>
      </header>

      <div className="card">
        <HStack gap={1} className="tabs">
          {TABS.map((tab) => (
            <Button
              key={tab.id}
              type="button"
              variant={activeTab === tab.id ? "secondary" : "ghost"}
              size="sm"
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </Button>
          ))}
        </HStack>

        <div className="tab-content">
          {activeTab === "items" && <ItemsTab />}
          {activeTab === "stores" && <StoresTab />}
          {activeTab === "stock" && <StockTab />}
          {activeTab === "movements" && (
            <p className="muted">Stock movements are recorded via the API.</p>
          )}
        </div>
      </div>
    </div>
  );
}
