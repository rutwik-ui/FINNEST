export const CONTENT_ROLES = ['super_admin', 'content_manager'];
export const CATEGORIES = ['general', 'new_property', 'resale_property'];
export function validateGuidance(input) {
  const title = input.title.trim();
  const body = input.body.trim();
  if (!title || title.length > 150) return 'Enter a title of 1–150 characters.';
  if (!body || body.length > 10000) return 'Enter guidance of 1–10,000 characters.';
  if (!CATEGORIES.includes(input.category)) return 'Choose a valid category.';
  if (!['draft', 'published'].includes(input.status)) return 'Choose a valid status.';
  return null;
}
