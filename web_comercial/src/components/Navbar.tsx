'use client';
import { useState, useEffect } from 'react';
import { Menu, X, TrendingUp } from 'lucide-react';
import styles from './Navbar.module.css';

const navLinks = [
  { label: 'Inicio', href: '#inicio' },
  { label: 'Productos', href: '#productos' },
  { label: 'Simulador', href: '#simulador' },
  { label: 'Nosotros', href: '#nosotros' },
  { label: 'Blog', href: '#blog' },
  { label: 'Contacto', href: '#contacto' },
];

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header className={`${styles.navbar} ${scrolled ? styles.scrolled : ''}`}>
      <nav className={`container ${styles.nav}`}>
        {/* Logo */}
        <a href="#inicio" className={styles.logo}>
          <div className={styles.logoIcon}>
            <TrendingUp size={20} color="#fff" />
          </div>
          <span className={styles.logoText}>MET</span>
        </a>

        {/* Desktop Links */}
        <ul className={styles.links}>
          {navLinks.map((link) => (
            <li key={link.href}>
              <a href={link.href} className={styles.link}>{link.label}</a>
            </li>
          ))}
        </ul>

        {/* CTA */}
        <div className={styles.actions}>
          <a href="#" className="btn-primary" style={{ padding: '10px 22px', fontSize: '14px' }}>
            Iniciar Sesión
          </a>
        </div>

        {/* Hamburger */}
        <button className={styles.hamburger} onClick={() => setMenuOpen(!menuOpen)} aria-label="Menú">
          {menuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </nav>

      {/* Mobile Menu */}
      {menuOpen && (
        <div className={styles.mobileMenu}>
          {navLinks.map((link) => (
            <a key={link.href} href={link.href} className={styles.mobileLink} onClick={() => setMenuOpen(false)}>
              {link.label}
            </a>
          ))}
          <a href="#" className="btn-primary" style={{ marginTop: '12px', justifyContent: 'center' }}>
            Iniciar Sesión
          </a>
        </div>
      )}
    </header>
  );
}
