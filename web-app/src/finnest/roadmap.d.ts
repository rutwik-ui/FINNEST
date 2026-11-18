export type RoadmapStep = { label: string; icon: string; path: string; section?: string };
export const NEW_ROADMAP: RoadmapStep[];
export const RESALE_ROADMAP: RoadmapStep[];
export function roadmapFor(journey: string): RoadmapStep[];
