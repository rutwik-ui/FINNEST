/** Project records remain browser-local until a secure sync feature is implemented. */
export const MAX_PROJECTS = 5;
export function cleanName(value) {
  return typeof value === 'string' ? value.trim().slice(0, 100) : '';
}
export function addProject(projects, name, id) {
  const clean = cleanName(name);
  if (!clean || projects.length >= MAX_PROJECTS) return projects;
  return [...projects, { id, name: clean }];
}
export function renameProject(projects, id, name) {
  const clean = cleanName(name);
  return clean ? projects.map(project => project.id === id ? { ...project, name: clean } : project) : projects;
}
export function removeProject(projects, id) {
  return projects.filter(project => project.id !== id);
}
