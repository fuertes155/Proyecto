'use client';
import { useEffect, useRef, useState } from 'react';
import { Users, Award, CreditCard, TrendingUp } from 'lucide-react';
import styles from './StatsSection.module.css';

const stats = [
  { icon: Users, value: 15000, suffix: '+', label: 'Socios Activos', color: 'var(--primary)' },
  { icon: Award, value: 12, suffix: ' años', label: 'De Experiencia', color: 'var(--accent-blue)' },
  { icon: CreditCard, value: 42000, suffix: '+', label: 'Créditos Otorgados', color: 'var(--accent-purple)' },
  { icon: TrendingUp, value: 85, suffix: 'B', label: 'En Activos Administrados', color: 'var(--accent-gold)' },
];

function CountUp({ target, suffix }: { target: number; suffix: string }) {
  const [count, setCount] = useState(0);
  const [started, setStarted] = useState(false);
  const ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting && !started) setStarted(true); },
      { threshold: 0.5 }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, [started]);

  useEffect(() => {
    if (!started) return;
    const duration = 2000;
    const steps = 60;
    const increment = target / steps;
    let current = 0;
    const timer = setInterval(() => {
      current += increment;
      if (current >= target) { setCount(target); clearInterval(timer); }
      else setCount(Math.floor(current));
    }, duration / steps);
    return () => clearInterval(timer);
  }, [started, target]);

  return <span ref={ref}>{count.toLocaleString('es-CO')}{suffix}</span>;
}

export default function StatsSection() {
  return (
    <section id="stats" className={styles.stats}>
      <div className={styles.bg} />
      <div className="container">
        <div className={styles.grid}>
          {stats.map(({ icon: Icon, value, suffix, label, color }) => (
            <div key={label} className={styles.item}>
              <div className={styles.iconWrap} style={{ background: `${color}18`, border: `1px solid ${color}30` }}>
                <Icon size={24} color={color} />
              </div>
              <div className={styles.number} style={{ color }}>
                <CountUp target={value} suffix={suffix} />
              </div>
              <div className={styles.label}>{label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
