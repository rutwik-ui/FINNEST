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
