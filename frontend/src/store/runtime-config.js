import { create } from "zustand";
import { queryTenantConfigs } from "../api/config";
import CONFIG_META from "../config/config-meta";

const STORAGE_KEY = "tenantRuntimeConfig";

function normalize(items) {
  const out = {};
  (items || []).forEach((row) => {
    if (row?.key) out[row.key] = row.value ?? "";
  });
  return out;
}

function withDefaults(raw) {
  const merged = { ...raw };
  Object.keys(CONFIG_META).forEach((key) => {
    if (!(key in merged)) {
      merged[key] = CONFIG_META[key].defaultValue ?? "";
    }
  });
  return merged;
}

const useRuntimeConfigStore = create((set, get) => ({
  values: withDefaults(JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}")),
  loaded: false,
  async refresh() {
    try {
      const res = await queryTenantConfigs({ page: 1, pageSize: 200 });
      const values = withDefaults(normalize(res.data?.data?.items || []));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
      set({ values, loaded: true });
      return values;
    } catch {
      set({ loaded: true });
      return get().values;
    }
  },
  patchValue(key, value) {
    const values = { ...get().values, [key]: value };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
    set({ values });
  },
}));

export default useRuntimeConfigStore;
