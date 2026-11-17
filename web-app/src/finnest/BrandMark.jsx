import React from 'react';
import { Home } from 'lucide-react';
/** Original FINNEST house glyph (the uploaded app icon is used only for favicon/launcher). */
export function BrandMark({size=30}){return <span className="brand-house" aria-hidden="true"><Home size={size}/></span>}
/** Original FINNEST wordmark: F + red house + NNEST. */
export function Wordmark({size=30,className=''}){return <span className={`brand-wordmark ${className}`} aria-label="FINNEST">F<BrandMark size={size}/>NNEST</span>}
