import { ArrowRight, Clock } from 'lucide-react';
import styles from './BlogSection.module.css';

const articles = [
  {
    tag: 'Finanzas Personales',
    tagColor: '#00C06F',
    title: '5 hábitos financieros que cambiarán tu relación con el dinero',
    excerpt: 'Aprende los principios que usan las personas más prósperas para gestionar sus ingresos, ahorrar sin esfuerzo y construir un patrimonio sólido desde cero.',
    date: 'Julio 2025',
    readTime: '4 min',
    emoji: '💡',
  },
  {
    tag: 'Cooperativismo',
    tagColor: '#4F8EF7',
    title: '¿Por qué una cooperativa puede ser mejor que un banco tradicional?',
    excerpt: 'Te explicamos las diferencias clave entre ser cliente de un banco y ser socio de una cooperativa: quién se beneficia realmente de tus ahorros.',
    date: 'Junio 2025',
    readTime: '6 min',
    emoji: '🏦',
  },
  {
    tag: 'Inversiones',
    tagColor: '#8B5CF6',
    title: 'CDTs en cooperativas: ¿son seguros y cuánto puedo ganar?',
    excerpt: 'Todo lo que necesitas saber sobre los Certificados de Depósito a Término, el seguro Fogacoop y cómo maximizar el rendimiento de tus ahorros.',
    date: 'Mayo 2025',
    readTime: '5 min',
    emoji: '📈',
  },
];

export default function BlogSection() {
  return (
    <section id="blog" className={`section ${styles.blog}`}>
      <div className="container">
        <div className={styles.header}>
          <div>
            <span className="section-label">Blog MET</span>
            <h2 className="section-title">Educación financiera<br /><span className="gradient-text">para todos</span></h2>
          </div>
          <a href="#" className="btn-secondary" style={{ alignSelf: 'flex-end' }}>
            Ver todos los artículos <ArrowRight size={16} />
          </a>
        </div>
        <div className={styles.grid}>
          {articles.map(({ tag, tagColor, title, excerpt, date, readTime, emoji }) => (
            <article key={title} className={styles.card}>
              <div className={styles.cardTop}>
                <div className={styles.emoji}>{emoji}</div>
                <span className={styles.tag} style={{ color: tagColor, background: `${tagColor}15`, border: `1px solid ${tagColor}30` }}>{tag}</span>
              </div>
              <h3 className={styles.title}>{title}</h3>
              <p className={styles.excerpt}>{excerpt}</p>
              <div className={styles.meta}>
                <Clock size={13} />
                <span>{date} · {readTime} de lectura</span>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
