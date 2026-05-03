import { create } from "zustand";

const useAppStore = create((set) => ({
  theme: "light",
  setTheme: (theme) => set({ theme }),
  sidebarMenuNonce: 0,
  bumpSidebarMenu: () => set((s) => ({ sidebarMenuNonce: s.sidebarMenuNonce + 1 })),
}));

export default useAppStore;

