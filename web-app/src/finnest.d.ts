declare module "@/finnest/FinnestApp" {
  export const App: import("react").ComponentType;
}

declare module "@/finnest/roadmap" {
  type RoadmapStep = { label: string; icon: string; path: string; section?: string };
  export const NEW_ROADMAP: RoadmapStep[];
  export const RESALE_ROADMAP: RoadmapStep[];
  export function roadmapFor(journey: string): RoadmapStep[];
}

declare module "@/finnest/AdminBulkImport" {
  const BulkImport: import("react").ComponentType<{ kind: 'guidance' | 'experts'; onImported?: () => void; disabled?: boolean }>;
  export default BulkImport;
}

declare module "@/finnest/admin" {
  export const ACCESS_ROLES: string[];
  export function canAccessAdmin(roles: string[]): boolean;
}

declare module "@/finnest/AdminConsole" {
  export function canOpen(roles: string[], section: string): boolean;
  const AdminConsole: import("react").ComponentType<{
    go: (path: string) => void;
    session: { user: { id: string; email?: string }; access_token: string } | null;
    authLoading: boolean;
  }>;
  export default AdminConsole;
}
