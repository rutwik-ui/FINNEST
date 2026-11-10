// Both roadmaps link only to existing subject areas; numbering is navigation, not verified completion.
export const NEW_ROADMAP = [
  { label: 'Requirements & budget', icon: 'ClipboardList', path: '/journey', section: 'requirements' },
  { label: 'Developer checks', icon: 'Search', path: '/journey', section: 'checks' },
  { label: 'Shortlist projects', icon: 'Bookmark', path: '/projects' },
  { label: 'Visit observations', icon: 'FileText', path: '/visit' },
  { label: 'Home loan guide', icon: 'Landmark', path: '/loan' },
  { label: 'Legal due diligence', icon: 'Handshake', path: '/legal' },
  { label: 'Financial due diligence', icon: 'IndianRupee', path: '/financial' },
  { label: 'Final review', icon: 'Check', path: '/review' },
];

export const RESALE_ROADMAP = [
  { label: 'Requirements & location', icon: 'ClipboardList', path: '/resale-requirements' },
  { label: 'Property condition', icon: 'Search', path: '/resale-condition' },
  { label: 'Seller, ownership & title', icon: 'UserRound', path: '/resale-seller' },
  { label: 'Society documents & dues', icon: 'Building2', path: '/resale-society' },
  { label: 'Legal review', icon: 'Handshake', path: '/resale-legal' },
  { label: 'Agreement & handover', icon: 'KeyRound', path: '/resale-handover' },
  { label: 'Journey summary', icon: 'FileText', path: '/resale-summary' },
];

export const roadmapFor = journey => journey === 'Resale Property' ? RESALE_ROADMAP : NEW_ROADMAP;