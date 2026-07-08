import { TrendingUp, Share2, Users, MessageCircle, Link, PlayCircle } from 'lucide-react';
import styles from './Footer.module.css';

const links = {
  Productos: ['Cuenta Digital', 'Créditos', 'Inversiones', 'Transferencias'],
  Empresa: ['Quiénes Somos', 'Blog', 'Prensa', 'Trabaja con Nosotros'],
  Legal: ['Términos y Condiciones', 'Política de Privacidad', 'Protección de Datos', 'Tarifas'],
  Soporte: ['Centro de Ayuda', 'Contacto', 'App Móvil', 'Sucursales'],
};

const socials = [
  { icon: Share2, label: 'Instagram' },
  { icon: Users, label: 'Facebook' },
  { icon: MessageCircle, label: 'Twitter' },
  { icon: Link, label: 'LinkedIn' },
  { icon: PlayCircle, label: 'YouTube' },
];

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={styles.top}>
        <div className="container">
          <div className={styles.grid}>
            {/* Brand */}
            <div className={styles.brand}>
              <div className={styles.logo}>
                <div className={styles.logoIcon}><TrendingUp size={18} color="#fff" /></div>
                <span className={styles.logoText}>MET</span>
              </div>
              <p className={styles.tagline}>
                La cooperativa financiera digital que trabaja para ti. Seguros, modernos y transparentes.
              </p>
              <div className={styles.socials}>
                {socials.map(({ icon: Icon, label }) => (
                  <a key={label} href="#" aria-label={label} className={styles.social}>
                    <Icon size={16} />
                  </a>
                ))}
              </div>
              <div className={styles.badges}>
                <span className={styles.badge}>🛡️ Vigilado por Superfinanciera</span>
                <span className={styles.badge}>🔒 Fogacoop</span>
              </div>
            </div>

            {/* Links */}
            {Object.entries(links).map(([section, items]) => (
              <div key={section} className={styles.linkGroup}>
                <h4 className={styles.linkTitle}>{section}</h4>
                <ul>
                  {items.map((item) => (
                    <li key={item}>
                      <a href="#" className={styles.link}>{item}</a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className={styles.bottom}>
        <div className="container">
          <p>© 2025 MET Cooperativa Financiera. Todos los derechos reservados.</p>
          <p>Desarrollado con 💚 en Colombia · NIT: 000.000.000-0</p>
        </div>
      </div>
    </footer>
  );
}
