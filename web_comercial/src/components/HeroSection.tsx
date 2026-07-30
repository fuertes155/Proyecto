'use client';
import { ArrowRight, Shield, Smartphone, ChevronDown } from 'lucide-react';
import styles from './HeroSection.module.css';

export default function HeroSection() {
  return (
    <section id="inicio" className={styles.hero}>
      {/* Background elements */}
      <div className={styles.bgGlow1} />
      <div className={styles.bgGlow2} />
      <div className={styles.grid} />

      {/* Orbiting circles */}
      <div className={styles.orbit1} />
      <div className={styles.orbit2} />

      <div className="container">
        <div className={styles.content}>
          {/* Badge */}
          <div className={styles.badge}>
            <Shield size={14} />
            <span>Plataforma 100% segura y regulada</span>
          </div>

          {/* Headline */}
          <h1 className={styles.title}>
            MET,{' '}
            <span className="gradient-text">trabaja para ti</span>
          </h1>

          <p className={styles.subtitle}>
            MET te ofrece servicios financieros digitales modernos — cuentas, 
            créditos, inversiones y transferencias — con la seguridad que 
            mereces y la tecnología que necesitas.
          </p>

          {/* CTAs */}
          <div className={styles.ctas}>
            <a href="#productos" className="btn-primary">
              Abrir mi cuenta
              <ArrowRight size={18} />
            </a>
            <a href="#simulador" className="btn-secondary">
              Simular mi crédito
            </a>
          </div>

          {/* Trust badges */}
          <div className={styles.trust}>
            <div className={styles.trustItem}>
              <Smartphone size={16} color="var(--primary)" />
              <span>App disponible en iOS y Android</span>
            </div>
            <div className={styles.trustDot} />
            <div className={styles.trustItem}>
              <Shield size={16} color="var(--primary)" />
              <span>Vigilado por Superfinanciera</span>
            </div>
          </div>
        </div>

        {/* Hero card visual */}
        <div className={styles.visual}>
          <div className={styles.card}>
            <div className={styles.cardHeader}>
              <div className={styles.cardLogo}>MET</div>
              <div className={styles.cardChip} />
            </div>
            <div className={styles.cardBalance}>
              <span className={styles.cardBalanceLabel}>Saldo disponible</span>
              <span className={styles.cardBalanceAmount}>$12,450,000</span>
            </div>
            <div className={styles.cardFooter}>
              <span>**** **** **** 4821</span>
              <span>Eval Samuel M.</span>
            </div>
          </div>

          {/* Floating notifications */}
          <div className={`${styles.notification} ${styles.notif1}`}>
            <div className={styles.notifIcon}>✅</div>
            <div>
              <div className={styles.notifTitle}>Transferencia exitosa</div>
              <div className={styles.notifSub}>$250,000 enviados</div>
            </div>
          </div>

          <div className={`${styles.notification} ${styles.notif2}`}>
            <div className={styles.notifIcon}>📈</div>
            <div>
              <div className={styles.notifTitle}>Inversión generando</div>
              <div className={styles.notifSub}>+$18,900 este mes</div>
            </div>
          </div>
        </div>
      </div>

      {/* Scroll indicator */}
      <a href="#stats" className={styles.scrollIndicator}>
        <ChevronDown size={20} />
      </a>
    </section>
  );
}
