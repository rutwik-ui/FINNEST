import React from 'react';
import { Home } from 'lucide-react';
import { logoGeometry } from './logoGeometry';
/** Original FINNEST house glyph (the uploaded app icon is used only for favicon/launcher). */
export function BrandMark({size=30}){return <span className="brand-house" aria-hidden="true"><Home size={size}/></span>}
/** Original FINNEST wordmark: F + red house + NNEST. */
export function Wordmark({size=30,className=''}){return <span className={`brand-wordmark ${className}`} role="img" aria-label="FINNEST"><svg className="brand-lockup" viewBox={`-1 -1 ${logoGeometry.width+2} 22`} width={logoGeometry.width} height={22} aria-hidden="true"><path className="brand-lockup-letters" d={logoGeometry.letters}/><path className="brand-lockup-house" d={logoGeometry.house}/></svg></span>}
