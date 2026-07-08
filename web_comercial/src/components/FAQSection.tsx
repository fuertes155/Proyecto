'use client';
import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import styles from './FAQSection.module.css';

const faqs = [
  {
    q: '¿Cómo me afilio a la Cooperativa MET?',
    a: 'Puedes afiliarte directamente desde nuestra app móvil o en cualquiera de nuestras sucursales. Solo necesitas tu documento de identidad y un aporte de vinculación. El proceso toma menos de 10 minutos.'
  },
  {
    q: '¿Mis ahorros están protegidos en MET?',
    a: 'Sí. La Cooperativa MET está vinculada a Fogacoop (Fondo de Garantías de Entidades Cooperativas), que protege tus depósitos hasta por 50 millones de pesos. Además, estamos vigilados por la Superfinanciera.'
  },
  {
    q: '¿Qué tasas de interés ofrecen para créditos?',
    a: 'Nuestras tasas son competitivas y personalizadas según el perfil de cada socio. Oscilan entre el 1.2% y el 2.1% mensual. Usa nuestro simulador de crédito para ver tu cuota estimada antes de solicitarlo.'
  },
  {
    q: '¿Puedo hacer transferencias a otros bancos desde MET?',
    a: 'Por supuesto. Puedes realizar transferencias PSE y ACH a cualquier banco o entidad financiera en Colombia directamente desde la app, sin costo adicional para socios activos.'
  },
  {
    q: '¿Qué es un CDT y cómo funciona en MET?',
    a: 'Un CDT (Certificado de Depósito a Término) es un instrumento donde depositas un monto por un tiempo determinado a cambio de una tasa de interés fija. En MET puedes abrir un CDT desde $100.000 con plazos de 30 a 360 días.'
  },
  {
    q: '¿Hay costos o comisiones ocultas en MET?',
    a: 'No. Creemos en la transparencia total. Los socios activos no pagan cuota de mantenimiento de cuenta. Todas las tarifas aplicables están publicadas en nuestra tabla de tarifas que puedes consultar en la app.'
  },
];

export default function FAQSection() {
  const [open, setOpen] = useState<number | null>(null);

  return (
    <section id="faq" className={`section ${styles.faq}`}>
      <div className="container">
        <div className={styles.header}>
          <span className="section-label">FAQ</span>
          <h2 className="section-title">
            Preguntas <span className="gradient-text">frecuentes</span>
          </h2>
          <p className="section-subtitle">
            Resolvemos las dudas más comunes de nuestros socios y futuros asociados.
          </p>
        </div>

        <div className={styles.list}>
          {faqs.map((faq, i) => (
            <div key={i} className={`${styles.item} ${open === i ? styles.itemOpen : ''}`}>
              <button className={styles.question} onClick={() => setOpen(open === i ? null : i)}>
                <span>{faq.q}</span>
                <ChevronDown size={20} className={`${styles.chevron} ${open === i ? styles.chevronOpen : ''}`} />
              </button>
              <div className={`${styles.answer} ${open === i ? styles.answerOpen : ''}`}>
                <p>{faq.a}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
