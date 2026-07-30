import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import Navbar from './Navbar';
import Footer from './Footer';
import styles from './LegalLayout.module.css';

export default function LegalLayout({
  title,
  updated,
  children,
}: {
  title: string;
  updated: string;
  children: React.ReactNode;
}) {
  return (
    <>
      <Navbar />
      <main className={styles.main}>
        <div className="container">
          <div className={styles.content}>
            <Link href="/" className={styles.back}>
              <ArrowLeft size={16} /> Volver al inicio
            </Link>
            <h1 className={styles.title}>{title}</h1>
            <p className={styles.updated}>Última actualización: {updated}</p>
            <div className={styles.body}>{children}</div>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
