import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import TerminosPage from './page';

describe('Página de Términos y Condiciones', () => {
  it('renderiza el título y el enlace de regreso al inicio', () => {
    render(<TerminosPage />);

    expect(
      screen.getByRole('heading', { level: 1, name: /términos y condiciones/i })
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /volver al inicio/i })).toHaveAttribute(
      'href',
      '/'
    );
  });
});
