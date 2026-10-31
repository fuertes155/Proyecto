import { describe, it, expect, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Navbar from './Navbar';
import FAQSection from './FAQSection';

describe('Navbar', () => {
  it('abre y cierra el menú móvil, y se cierra al elegir una sección', async () => {
    const user = userEvent.setup();
    render(<Navbar />);
    // En escritorio el botón se oculta por CSS; la prueba simula el celular.
    const toggle = screen.getByLabelText('Menú');
    const linksBefore = screen.getAllByRole('link').length;

    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const mobileLinks = screen.getAllByRole('link').slice(linksBefore);
    expect(mobileLinks.length).toBeGreaterThan(0);

    await user.click(mobileLinks[0]);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getAllByRole('link')).toHaveLength(linksBefore);
  });

  it('cambia de estilo al bajar por la página y vuelve al subir hasta arriba', () => {
    render(<Navbar />);
    const barra = screen.getByRole('banner');
    const arriba = barra.className;

    act(() => {
      window.scrollY = 300;
      window.dispatchEvent(new Event('scroll'));
    });
    expect(barra.className).toMatch(/scrolled/);

    act(() => {
      window.scrollY = 0;
      window.dispatchEvent(new Event('scroll'));
    });
    expect(barra.className).toBe(arriba);
  });

  it('deja de escuchar el scroll al salir de la página', () => {
    const quitar = vi.spyOn(window, 'removeEventListener');
    const { unmount } = render(<Navbar />);

    unmount();

    expect(quitar).toHaveBeenCalledWith('scroll', expect.any(Function));
    quitar.mockRestore();
  });
});

describe('FAQSection', () => {
  it('abre una pregunta a la vez y la cierra al tocarla de nuevo', async () => {
    const user = userEvent.setup();
    render(<FAQSection />);
    const [first, second] = screen.getAllByRole('button');
    expect(first).toHaveAttribute('aria-expanded', 'false');

    await user.click(first);
    expect(first).toHaveAttribute('aria-expanded', 'true');

    await user.click(second);
    expect(first).toHaveAttribute('aria-expanded', 'false');
    expect(second).toHaveAttribute('aria-expanded', 'true');

    await user.click(second);
    expect(second).toHaveAttribute('aria-expanded', 'false');
  });
});
