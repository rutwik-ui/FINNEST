/** The screenshot's visible visit prompts; status choices remain provisional. */
export const VISIT_CHECKS = [
  ['carpet_area', 'Carpet area'], ['light', 'Light'], ['ventilation', 'Ventilation'],
  ['noise', 'Noise'], ['water', 'Water'], ['power_backup', 'Power backup'],
  ['parking', 'Parking'], ['lifts', 'Lifts'], ['fire_safety', 'Fire-safety provisions'],
  ['elderly_access', 'Elderly access'], ['security', 'Security'],
  ['construction', 'Visible construction'], ['open_space', 'Open space'], ['garden', 'Garden / play area'],
];
export function updateVisitCheck(checks, id, field, value) {
  return { ...checks, [id]: { ...(checks[id] || {}), [field]: value } };
}
