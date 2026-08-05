import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import PrivacidadPage from './page';

describe('Página de Política de Privacidad', () => {
  it('renderiza el título y las secciones de Habeas Data', () => {
    render(<PrivacidadPage />);

    expect(
      screen.getByRole('heading', { level: 1, name: /política de privacidad/i })
    ).toBeInTheDocument();
    expect(screen.getAllByText(/ley 1581 de 2012/i).length).toBeGreaterThan(0);
    expect(
      screen.getByRole('heading', { name: /sus derechos \(habeas data\)/i })
    ).toBeInTheDocument();
  });
});
