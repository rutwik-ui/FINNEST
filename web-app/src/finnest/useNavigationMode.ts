import { useEffect, useState } from 'react';
import { navigationMode } from './navigation';
export function useNavigationMode() {
  const [mode, setMode] = useState(() => navigationMode(window.innerWidth));
  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 1024px)');
    const tablet = window.matchMedia('(min-width: 768px)');
    const update = () => setMode(navigationMode(window.innerWidth));
    desktop.addEventListener('change', update); tablet.addEventListener('change', update);
    return () => { desktop.removeEventListener('change', update); tablet.removeEventListener('change', update); };
  }, []);
  return mode;
}