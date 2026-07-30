'use client';
import { useState } from 'react';
import { MapPin, Phone, Mail, Send } from 'lucide-react';
import styles from './ContactSection.module.css';

export default function ContactSection() {
  const [form, setForm] = useState({ name: '', email: '', message: '' });
  const [sent, setSent] = useState(false);

  const [error, setError] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}/support/public/contact`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form)
      });
      if (!res.ok) throw new Error('Error al enviar mensaje');
      setSent(true);
      setError(false);
      setForm({ name: '', email: '', message: '' });
      setTimeout(() => setSent(false), 5000);
    } catch (err) {
      setError(true);
    }
  };

  return (
    <section id="contacto" className={`section ${styles.contact}`}>
      <div className="container">
        <div className={styles.header}>
          <span className="section-label">Contáctenos</span>
          <h2 className="section-title">
            Estamos aquí para <span className="gradient-text">ayudarte</span>
          </h2>
          <p className="section-subtitle">
            Visítanos en nuestras oficinas, llámanos o escríbenos. Un asesor MET estará feliz de atenderte.
          </p>
        </div>

        <div className={styles.grid}>
          {/* Info */}
          <div className={styles.info}>
            <div className={styles.infoCard}>
              <div className={styles.infoIcon}><MapPin size={20} color="var(--primary)" /></div>
              <div>
                <h4>Oficina Principal</h4>
                <p>Calle 10 # 5-32, Piso 3<br />Colombia</p>
              </div>
            </div>
            <div className={styles.infoCard}>
              <div className={styles.infoIcon}><Phone size={20} color="var(--primary)" /></div>
              <div>
                <h4>Teléfono</h4>
                <p>+57 315 859 8604<br />Lunes a Viernes 8am – 5pm</p>
              </div>
            </div>
            <div className={styles.infoCard}>
              <div className={styles.infoIcon}><Mail size={20} color="var(--primary)" /></div>
              <div>
                <h4>Correo Electrónico</h4>
                <p>contacto@met.com.co</p>
              </div>
            </div>

            {/* Map placeholder */}
            <div className={styles.map}>
              <div className={styles.mapPlaceholder}>
                <MapPin size={32} color="var(--primary)" />
                <span>Mapa de ubicación</span>
              </div>
            </div>
          </div>

          {/* Form */}
          <div className={styles.formCard}>
            <h3 className={styles.formTitle}>Envíanos un mensaje</h3>
            {sent ? (
              <div className={styles.success}>
                <span>✅</span>
                <div>
                  <strong>¡Mensaje enviado!</strong>
                  <p>Nos pondremos en contacto contigo pronto.</p>
                </div>
              </div>
            ) : (
              <form className={styles.form} onSubmit={handleSubmit}>
                {error && (
                  <div className={styles.errorBox}>
                    Hubo un problema al enviar el mensaje. Por favor, inténtalo de nuevo.
                  </div>
                )}
                <div className={styles.field}>
                  <label>Nombre completo</label>
                  <input type="text" placeholder="Ej. Juan García" required
                    value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
                </div>
                <div className={styles.field}>
                  <label>Correo electrónico</label>
                  <input type="email" placeholder="correo@ejemplo.com" required
                    value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
                </div>
                <div className={styles.field}>
                  <label>Mensaje</label>
                  <textarea placeholder="¿En qué podemos ayudarte?" rows={5} required
                    value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} />
                </div>
                <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
                  <Send size={16} /> Enviar mensaje
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
