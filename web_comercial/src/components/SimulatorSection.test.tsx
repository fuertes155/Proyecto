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
    // La simulación de crédito pudo arrancar antes del clic (debounce de 300 ms); lo que importa es
    // que el ahorro no pida nada más.
    const callsAfterSwitch = vi.mocked(global.fetch).mock.calls.length;
    await new Promise((resolve) => setTimeout(resolve, 400));

    expect(screen.getByText(/aporte mensual/i)).toBeInTheDocument();
    expect(screen.queryByText(/monto del crédito/i)).not.toBeInTheDocument();
    // El simulador de ahorro es cálculo local (no depende de la API del backend)
    expect(global.fetch).toHaveBeenCalledTimes(callsAfterSwitch);
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

  it('simula el crédito en el backend con el monto y el plazo elegidos', async () => {
    render(<SimulatorSection />);

    expect(await screen.findByText(/250\.000/)).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [url, init] = vi.mocked(global.fetch).mock.calls[0];
    expect(String(url)).toMatch(/\/v1\/loans\/simulate$/);
    expect(JSON.parse(String(init?.body))).toEqual({ amount: 5000000, termMonths: 24 });
    expect(screen.getByText('1.50%')).toBeInTheDocument();
  });

  it('si el backend no puede simular, lo dice en vez de quedarse cargando', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: false, status: 401, json: () => Promise.resolve({}) })));
    render(<SimulatorSection />);

    expect(await screen.findByText(/no pudimos calcular la cuota/i)).toBeInTheDocument();
  });

  it('cada aporte de ahorro gana interés solo por los meses que lleva ahorrado', async () => {
    render(<SimulatorSection />);
    await userEvent.setup().click(screen.getByRole('button', { name: /simulador de ahorro/i }));

    // 12 aportes de $500.000 al 0,8 % mensual: el primero rinde 12 meses y el último 1,
    // así que los intereses son 500.000 × 0,008 × (12 + 11 + … + 1) = $312.000, no $576.000.
    expect(screen.getByText(/^\$\s*312\.000$/)).toBeInTheDocument();
    expect(screen.getByText(/6\.312\.000/)).toBeInTheDocument();
    expect(screen.queryByText(/576\.000/)).not.toBeInTheDocument();
  });
});
