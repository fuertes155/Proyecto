import Link from 'next/link';
import { TrendingUp, Share2, Users, MessageCircle, Link as LinkIcon, PlayCircle } from 'lucide-react';
import styles from './Footer.module.css';

// Links con rutas reales para los items legales
const legalLinks = [
  { label: 'Términos y Condiciones', href: '/terminos' },
  { label: 'Política de Privacidad', href: '/privacidad' },
  { label: 'Protección de Datos', href: '/privacidad#habeas-data' },
  { label: 'Tarifas', href: '/#productos' },
];

const links: Record<string, { label: string; href: string }[]> = {
  Productos: [
    { label: 'Cuenta Digital', href: '/#productos' },
    { label: 'Créditos', href: '/#productos' },
    { label: 'Inversiones', href: '/#productos' },
    { label: 'Transferencias', href: '/#productos' },
  ],
  Empresa: [
    { label: 'Quiénes Somos', href: '/#nosotros' },
    { label: 'Blog', href: '/#blog' },
    { label: 'Prensa', href: '#' },
    { label: 'Trabaja con Nosotros', href: '#' },
  ],
  Legal: legalLinks,
  Soporte: [
    { label: 'Centro de Ayuda', href: '/#faq' },
    { label: 'Contacto', href: '/#contacto' },
    { label: 'App Móvil', href: '#' },
    { label: 'Sucursales', href: '#' },
  ],
};

const socials = [
  { icon: Share2, label: 'Instagram' },
  { icon: Users, label: 'Facebook' },
  { icon: MessageCircle, label: 'Twitter' },
  { icon: LinkIcon, label: 'LinkedIn' },
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
                La financiera digital que trabaja para ti. Seguros, modernos y transparentes.
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
                    <li key={item.label}>
                      <Link href={item.href} className={styles.link}>{item.label}</Link>
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
          <p>© 2025 MET Financiera. Todos los derechos reservados.</p>
          <p>Desarrollado con 💚 en Colombia · NIT: 000.000.000-0</p>
        </div>
      </div>
    </footer>
  );
}
