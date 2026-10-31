import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import StatsSection from './StatsSection';

// Observador que la prueba controla: decide cuándo la sección "entra en pantalla".
let observadores: ObservadorControlado[] = [];

class ObservadorControlado {
  readonly observe = vi.fn();
  readonly unobserve = vi.fn();
  readonly disconnect = vi.fn();
  readonly takeRecords = vi.fn(() => []);
  constructor(readonly callback: IntersectionObserverCallback) {
    observadores.push(this);
  }
}

function seVeEnPantalla(visible = true) {
  act(() => {
    for (const o of observadores) {
      o.callback([{ isIntersecting: visible } as IntersectionObserverEntry], o as unknown as IntersectionObserver);
    }
  });
}

function pasan(ms: number) {
  act(() => {
    vi.advanceTimersByTime(ms);
  });
}

beforeEach(() => {
  observadores = [];
  vi.useFakeTimers();
  vi.stubGlobal('IntersectionObserver', ObservadorControlado);
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('StatsSection', () => {
  it('muestra las cuatro cifras de la cooperativa en cero hasta que la sección se ve', () => {
    render(<StatsSection />);
    pasan(5000);

    for (const etiqueta of ['Socios Activos', 'De Experiencia', 'Créditos Otorgados', 'En Activos Administrados']) {
      expect(screen.getByText(etiqueta)).toBeInTheDocument();
    }
    expect(screen.getAllByText(/^0(\+| años|B)$/)).toHaveLength(4);
  });

  it('al verse en pantalla cuenta hasta cada cifra en unos dos segundos', () => {
    render(<StatsSection />);

    seVeEnPantalla();
    pasan(1000);
    expect(screen.queryByText('15.000+')).not.toBeInTheDocument(); // a mitad de camino todavía cuenta
    pasan(1200);

    expect(screen.getByText('15.000+')).toBeInTheDocument();
    expect(screen.getByText('12 años')).toBeInTheDocument();
    expect(screen.getByText('42.000+')).toBeInTheDocument();
    expect(screen.getByText('85B')).toBeInTheDocument();
  });

  it('si la sección pasa de largo sin verse, no arranca el conteo', () => {
    render(<StatsSection />);

    seVeEnPantalla(false);
    pasan(3000);

    expect(screen.queryByText('15.000+')).not.toBeInTheDocument();
  });

  it('deja de observar la pantalla al salir de la página', () => {
    const { unmount } = render(<StatsSection />);

    unmount();

    expect(observadores.length).toBeGreaterThan(0);
    for (const o of observadores) expect(o.disconnect).toHaveBeenCalled();
  });
});
