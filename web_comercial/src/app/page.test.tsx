import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import Home from './page';

// SimulatorSection dispara un fetch al montar (tab "crédito"); lo simulamos para que el
// smoke test no dependa de una red real ni deje una promesa sin resolver.
beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            monthlyPayment: 0,
            totalPayment: 0,
            totalInterest: 0,
            monthlyInterestRate: 0,
          }),
      })
    ) as unknown as typeof fetch
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('Home (página pública)', () => {
  it('renderiza sin lanzar errores y muestra las secciones principales', () => {
    render(<Home />);

    // Navbar (role "banner" = <header>)
    expect(within(screen.getByRole('banner')).getByText('MET')).toBeInTheDocument();
    // Al menos un heading de sección relevante está presente
    expect(
      screen.getByRole('heading', { name: /calcula antes de decidir/i })
    ).toBeInTheDocument();
    // Footer (via role contentinfo si existe, si no basta con que no truene el render)
    expect(screen.getByText(/iniciar sesión/i)).toBeInTheDocument();
  });
});
