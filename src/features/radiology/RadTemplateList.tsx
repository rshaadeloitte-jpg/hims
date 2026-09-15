import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useRadTemplates,
  useCreateRadTemplate,
  type RadTemplate,
} from "./queries";
import { radTemplateSchema, type RadTemplateValues } from "./zodSchema";
import { MODALITIES, type Modality } from "./types";

/**
 * M16 RadTemplateList — Linear-style template management.
 * Left: filterable template list with modality badges. Right: create/edit form.
 */
export function RadTemplateList() {
  const [modalityFilter, setModalityFilter] = useState<Modality | "">("");
  const [editingTemplate, setEditingTemplate] = useState<RadTemplate | null>(null);
  const [showForm, setShowForm] = useState(false);

  const { data: templates = [], isLoading } = useRadTemplates(modalityFilter || undefined);
  const createTemplate = useCreateRadTemplate();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RadTemplateValues>({
    resolver: zodResolver(radTemplateSchema),
    defaultValues: {
      templateCode: "",
      modality: "XRAY",
      bodyTemplate: "",
      description: "",
    },
  });

  const handleNew = useCallback(() => {
    setEditingTemplate(null);
    reset({ templateCode: "", modality: "XRAY", bodyTemplate: "", description: "" });
    setShowForm(true);
  }, [reset]);

  const handleEdit = useCallback(
    (t: RadTemplate) => {
      setEditingTemplate(t);
      reset({
        templateCode: t.templateCode,
        modality: t.modality as Modality,
        bodyTemplate: t.bodyTemplate,
        description: t.description ?? "",
      });
      setShowForm(true);
    },
    [reset],
  );

  const onSubmit = async (values: RadTemplateValues) => {
    try {
      await createTemplate.mutateAsync(values as Record<string, unknown>);
      setShowForm(false);
      setEditingTemplate(null);
      reset();
    } catch (err) {
      console.error("Save failed:", err);
    }
  };

  const filteredTemplates = modalityFilter
    ? templates.filter((t) => t.modality === modalityFilter)
    : templates;

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Radiology Templates</h2>
          <p className="text-sm text-gray-500">Manage report templates by modality</p>
        </div>
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          onClick={handleNew}
        >
          New Template
        </button>
      </header>

      {/* Filters */}
      <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
        <div className="flex-1">
          <label htmlFor="filter-modality" className="sr-only">Modality</label>
          <select
            id="filter-modality"
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm"
            value={modalityFilter}
            onChange={(e) => setModalityFilter(e.target.value as Modality | "")}
          >
            <option value="">All Modalities</option>
            {MODALITIES.map((m) => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
        </div>
        <div className="text-sm text-gray-500">
          {filteredTemplates.length} template{filteredTemplates.length !== 1 ? "s" : ""}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Template List */}
        <div className="lg:col-span-2 border border-gray-200 rounded-lg overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
            <h3 className="text-sm font-medium text-gray-900">
              Templates ({filteredTemplates.length})
            </h3>
          </div>
          {isLoading ? (
            <div className="p-8 text-center text-gray-500">Loading templates...</div>
          ) : filteredTemplates.length === 0 ? (
            <div className="p-8 text-center text-gray-500">No templates found</div>
          ) : (
            <div className="divide-y divide-gray-200 max-h-[600px] overflow-y-auto">
              {filteredTemplates.map((t) => (
                <div
                  key={t.id}
                  className="p-4 hover:bg-gray-50 cursor-pointer transition-colors"
                  onClick={() => handleEdit(t)}
                >
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-900">{t.templateCode}</span>
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                        {t.modality}
                      </span>
                    </div>
                    <span className="text-xs font-mono text-gray-400">#{t.id}</span>
                  </div>
                  {t.description && (
                    <p className="text-sm text-gray-600 mb-1">{t.description}</p>
                  )}
                  <p className="text-xs text-gray-400 font-mono line-clamp-2">
                    {t.bodyTemplate}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Create/Edit Form */}
        {showForm && (
          <div className="border border-gray-200 rounded-lg p-4">
            <h3 className="text-sm font-medium text-gray-900 mb-4">
              {editingTemplate ? `Edit: ${editingTemplate.templateCode}` : "New Template"}
            </h3>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div>
                <label htmlFor="tpl-code" className="block text-xs font-medium text-gray-700 mb-1">
                  Template Code *
                </label>
                <input
                  id="tpl-code"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  placeholder="e.g. CT-HEAD-TRAUMA"
                  {...register("templateCode")}
                />
                {errors.templateCode && (
                  <p className="text-xs text-red-600 mt-1">{errors.templateCode.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="tpl-modality" className="block text-xs font-medium text-gray-700 mb-1">
                  Modality *
                </label>
                <select
                  id="tpl-modality"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...register("modality")}
                >
                  {MODALITIES.map((m) => (
                    <option key={m} value={m}>{m}</option>
                  ))}
                </select>
                {errors.modality && (
                  <p className="text-xs text-red-600 mt-1">{errors.modality.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="tpl-body" className="block text-xs font-medium text-gray-700 mb-1">
                  Body Template *
                </label>
                <textarea
                  id="tpl-body"
                  rows={8}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm font-mono"
                  placeholder="Enter template body with section headers..."
                  {...register("bodyTemplate")}
                />
                {errors.bodyTemplate && (
                  <p className="text-xs text-red-600 mt-1">{errors.bodyTemplate.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="tpl-desc" className="block text-xs font-medium text-gray-700 mb-1">
                  Description
                </label>
                <input
                  id="tpl-desc"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  placeholder="Optional description"
                  {...register("description")}
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm"
                  disabled={createTemplate.isPending}
                >
                  {createTemplate.isPending ? "Saving..." : editingTemplate ? "Update" : "Create"}
                </button>
                <button
                  type="button"
                  className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 text-sm"
                  onClick={() => { setShowForm(false); setEditingTemplate(null); }}
                >
                  Cancel
                </button>
              </div>

              {createTemplate.isError && (
                <div className="p-2 bg-red-50 border border-red-200 rounded text-xs text-red-700">
                  Save failed. Please try again.
                </div>
              )}
            </form>
          </div>
        )}

        {!showForm && (
          <div className="border border-dashed border-gray-300 rounded-lg p-8 text-center">
            <p className="text-sm text-gray-500">Select a template to edit, or click "New Template" to create one.</p>
          </div>
        )}
      </div>
    </div>
  );
}
