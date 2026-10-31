import { describe, it, expect, vi, afterEach } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ContactSection from './ContactSection';

afterEach(() => {
  vi.unstubAllGlobals();
});

async function fillAndSend() {
  const user = userEvent.setup();
  await user.type(screen.getByPlaceholderText('Ej. Juan García'), 'Laura Gómez');
  await user.type(screen.getByPlaceholderText('correo@ejemplo.com'), 'laura@example.com');
  await user.type(screen.getByPlaceholderText('¿En qué podemos ayudarte?'), 'Quiero abrir una cuenta');
  await user.click(screen.getByRole('button', { name: /enviar mensaje/i }));
}

describe('ContactSection', () => {
  it('envía el mensaje al backend y confirma al visitante', async () => {
    const fetchMock = vi.fn(() => Promise.resolve({ ok: true }));
    vi.stubGlobal('fetch', fetchMock);
    render(<ContactSection />);

    await fillAndSend();

    expect(await screen.findByText('¡Mensaje enviado!')).toBeInTheDocument();
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toMatch(/\/support\/public\/contact$/);
    expect(JSON.parse(String(init.body))).toEqual({
      name: 'Laura Gómez',
      email: 'laura@example.com',
      message: 'Quiero abrir una cuenta',
    });
  });

  it('si el envío falla avisa y conserva lo que escribió el visitante', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: false, status: 401 })));
    render(<ContactSection />);

    await fillAndSend();

    expect(await screen.findByText(/hubo un problema al enviar el mensaje/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText('¿En qué podemos ayudarte?')).toHaveValue('Quiero abrir una cuenta');
  });

  it('la confirmación desaparece a los 5 segundos y el formulario queda listo y vacío', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })));
      render(<ContactSection />);
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      await user.type(screen.getByPlaceholderText('Ej. Juan García'), 'Laura Gómez');
      await user.type(screen.getByPlaceholderText('correo@ejemplo.com'), 'laura@example.com');
      await user.type(screen.getByPlaceholderText('¿En qué podemos ayudarte?'), 'Quiero abrir una cuenta');
      await user.click(screen.getByRole('button', { name: /enviar mensaje/i }));
      expect(await screen.findByText('¡Mensaje enviado!')).toBeInTheDocument();

      act(() => {
        vi.advanceTimersByTime(5000);
      });

      expect(screen.queryByText('¡Mensaje enviado!')).not.toBeInTheDocument();
      expect(screen.getByPlaceholderText('Ej. Juan García')).toHaveValue('');
      expect(screen.getByPlaceholderText('¿En qué podemos ayudarte?')).toHaveValue('');
    } finally {
      vi.useRealTimers();
    }
  });

  it('sin conexión también avisa del problema', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))));
    render(<ContactSection />);

    await fillAndSend();

    expect(await screen.findByText(/hubo un problema al enviar el mensaje/i)).toBeInTheDocument();
  });
});
