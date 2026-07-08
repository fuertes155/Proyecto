import { Heart, Target, Shield, Zap } from 'lucide-react';
import styles from './AboutSection.module.css';

const values = [
  { icon: Heart, label: 'Solidaridad', desc: 'Crecemos juntos como comunidad.' },
  { icon: Target, label: 'Transparencia', desc: 'Cada centavo, rendición de cuentas.' },
  { icon: Shield, label: 'Seguridad', desc: 'Tus ahorros protegidos por Fogacoop.' },
  { icon: Zap, label: 'Innovación', desc: 'Tecnología al servicio del cooperativismo.' },
];

export default function AboutSection() {
  return (
    <section id="nosotros" className={`section ${styles.about}`}>
      <div className="container">
        <div className={styles.grid}>
          {/* Text */}
          <div className={styles.content}>
            <span className="section-label">Quiénes Somos</span>
            <h2 className="section-title">
              Más que una cooperativa,<br />
              <span className="gradient-text">somos tu familia financiera</span>
            </h2>
            <p className={styles.text}>
              MET nació con una convicción simple: que todos merecen acceso a servicios financieros dignos, 
              modernos y justos. Fundada por un grupo de personas con visión de futuro, hoy somos una 
              entidad regulada que combina los principios del cooperativismo con la potencia de la tecnología.
            </p>
            <p className={styles.text}>
              Cada socio que se une a MET no es un cliente — es un propietario. Participas en las 
              decisiones, compartes los excedentes y construyes junto a nosotros la cooperativa del futuro.
            </p>
            <div className={styles.mission}>
              <div className={styles.missionItem}>
                <h4>Nuestra Misión</h4>
                <p>Brindar servicios financieros cooperativos accesibles, digitales y seguros para mejorar el bienestar económico de nuestros socios.</p>
              </div>
              <div className={styles.missionItem}>
                <h4>Nuestra Visión</h4>
                <p>Ser la cooperativa financiera digital líder en Colombia, reconocida por innovación, solidez y compromiso social.</p>
              </div>
            </div>
          </div>

          {/* Values visual */}
          <div className={styles.visual}>
            <div className={styles.valuesGrid}>
              {values.map(({ icon: Icon, label, desc }) => (
                <div key={label} className={styles.valueCard}>
                  <div className={styles.valueIcon}>
                    <Icon size={22} color="var(--primary)" />
                  </div>
                  <h4 className={styles.valueLabel}>{label}</h4>
                  <p className={styles.valueDesc}>{desc}</p>
                </div>
              ))}
            </div>
            <div className={styles.bgDecor} />
          </div>
        </div>
      </div>
    </section>
  );
}
