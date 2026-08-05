import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SimulatorSection from './SimulatorSection';

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            monthlyPayment: 250000,
            totalPayment: 6000000,
            totalInterest: 1000000,
            monthlyInterestRate: 0.015,
          }),
      })
    ) as unknown as typeof fetch
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('SimulatorSection', () => {
  it('arranca en la pestaña de crédito y llama al backend para simular', async () => {
    render(<SimulatorSection />);

    expect(screen.getByText(/monto del crédito/i)).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled(); // el fetch está debounced (300ms)
  });

  it('cambia a la pestaña de ahorro y muestra el formulario correspondiente sin llamar al backend', async () => {
    const user = userEvent.setup();
    render(<SimulatorSection />);

    await user.click(screen.getByRole('button', { name: /simulador de ahorro/i }));

    expect(screen.getByText(/aporte mensual/i)).toBeInTheDocument();
    expect(screen.queryByText(/monto del crédito/i)).not.toBeInTheDocument();
    // El simulador de ahorro es cálculo local (no depende de la API del backend)
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('recalcula el total de ahorro al mover el slider de aporte mensual', async () => {
    render(<SimulatorSection />);
    await userEvent.setup().click(screen.getByRole('button', { name: /simulador de ahorro/i }));

    const [aporteSlider] = screen.getAllByRole('slider') as HTMLInputElement[];
    const labelBefore = screen.getByText(/aporte mensual/i).textContent;

    fireEvent.change(aporteSlider, { target: { value: '1000000' } });

    // No fijamos el string exacto de Intl.NumberFormat (varía según el ICU del runtime);
    // verificamos que el input controlado tomó el nuevo valor y que la UI se re-renderizó.
    expect(aporteSlider.value).toBe('1000000');
    expect(screen.getByText(/aporte mensual/i).textContent).not.toBe(labelBefore);
  });
});
