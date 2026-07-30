import { Wallet, BadgeDollarSign, TrendingUp, Send, ArrowRight } from 'lucide-react';
import styles from './ProductsSection.module.css';

const products = [
  {
    icon: Wallet,
    title: 'Cuenta Digital',
    description: 'Abre tu cuenta sin filas ni papeleo. Administra tu dinero desde la app con total seguridad.',
    features: ['Sin costo de mantenimiento', 'Transferencias gratuitas', 'Tarjeta virtual incluida'],
  },
  {
    icon: BadgeDollarSign,
    title: 'Créditos',
    description: 'Créditos de consumo y libre inversión con las tasas más competitivas del mercado.',
    features: ['Desde $500.000', 'Plazo hasta 60 meses', 'Respuesta en 24 horas'],
    featured: true,
  },
  {
    icon: TrendingUp,
    title: 'Inversiones / CDTs',
    description: 'Haz que tu dinero trabaje para ti con nuestros CDTs y planes de ahorro programado.',
    features: ['Tasas superiores al promedio', 'Desde $100.000', 'Seguro de depósito Fogacoop'],
  },
  {
    icon: Send,
    title: 'Transferencias',
    description: 'Envía y recibe dinero al instante a cualquier banco en Colombia, sin comisiones ocultas.',
    features: ['PSE y ACH', 'Sin límite de transacciones', 'Notificación en tiempo real'],
  },
];

export default function ProductsSection() {
  return (
    <section id="productos" className={`section ${styles.products}`}>
      <div className="container">
        <div className={styles.header}>
          <span className="section-label">Nuestros Productos</span>
          <h2 className="section-title">
            Todo lo que necesitas,<br />
            <span className="gradient-text">en un solo lugar</span>
          </h2>
          <p className="section-subtitle">
            Desde abrir tu cuenta hasta invertir tus ahorros — MET tiene el producto financiero ideal para cada etapa de tu vida.
          </p>
        </div>

        <div className={styles.grid}>
          {products.map(({ icon: Icon, title, description, features, featured }) => (
            <div key={title} className={`${styles.card} ${featured ? styles.featured : ''}`}>
              {featured && <div className={styles.featuredBadge}>Más popular</div>}
              <div className={styles.iconWrap}>
                <Icon size={26} color="var(--primary)" />
              </div>
              <h3 className={styles.title}>{title}</h3>
              <p className={styles.desc}>{description}</p>
              <ul className={styles.features}>
                {features.map((f) => (
                  <li key={f} className={styles.feature}>
                    <span className={styles.check}>✓</span>
                    {f}
                  </li>
                ))}
              </ul>
              <a href="#contacto" className={styles.cta}>
                Conocer más <ArrowRight size={16} />
              </a>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
