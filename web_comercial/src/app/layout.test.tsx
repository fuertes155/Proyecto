import { describe, it, expect } from 'vitest';
import type { ReactElement } from 'react';
import RootLayout, { metadata } from './layout';

describe('RootLayout', () => {
  it('declara la página en español para navegadores y lectores de pantalla', () => {
    const html = RootLayout({ children: <p>Contenido</p> }) as ReactElement<{ lang: string; children: ReactElement }>;

    expect(html.type).toBe('html');
    expect(html.props.lang).toBe('es');
    expect(html.props.children.type).toBe('body');
  });

  it('se presenta en buscadores y redes como MET, en español de Colombia', () => {
    expect(metadata.title).toBe('MET | Tu dinero, construyendo futuro');
    expect(metadata.description).toMatch(/créditos, inversiones y transferencias/);
    expect(metadata.openGraph).toMatchObject({ locale: 'es_CO', type: 'website' });
    expect(metadata.robots).toBe('index, follow');
  });
});
