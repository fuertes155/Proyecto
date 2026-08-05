import { vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

// jsdom no implementa IntersectionObserver (usado por StatsSection para animar contadores
// al hacer scroll). Stub mínimo para que los componentes que lo usan puedan montarse en tests.
class IntersectionObserverStub implements IntersectionObserver {
  readonly root: Element | Document | null = null;
  readonly rootMargin: string = '';
  readonly thresholds: ReadonlyArray<number> = [];
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }
}

vi.stubGlobal('IntersectionObserver', IntersectionObserverStub);
