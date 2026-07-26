'use client';
import { useState, useEffect } from 'react';
import styles from './SimulatorSection.module.css';

function formatCOP(value: number) {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value);
}

export default function SimulatorSection() {
  const [tab, setTab] = useState<'credito' | 'ahorro'>('credito');
  // Crédito
  const [monto, setMonto] = useState(5000000);
  const [plazo, setPlazo] = useState(24);
  const [creditoResult, setCreditoResult] = useState({
    cuota: 0,
    totalPagar: 0,
    totalIntereses: 0,
    tasa: 0,
    loading: false
  });

  // Ahorro
  const [aporte, setAporte] = useState(500000);
  const [meses, setMeses] = useState(12);
  const [tasaAhorro] = useState(0.8);

  const totalAhorro = aporte * meses;
  const interesesAhorro = totalAhorro * (tasaAhorro / 100) * meses;
  const totalFinal = totalAhorro + interesesAhorro;

  useEffect(() => {
    if (tab !== 'credito') return;
    
    const fetchSimulation = async () => {
      setCreditoResult(prev => ({ ...prev, loading: true }));
      try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}/v1/loans/simulate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ amount: monto, termMonths: plazo })
        });
        
        if (res.ok) {
          const data = await res.json();
          setCreditoResult({
            cuota: data.monthlyPayment,
            totalPagar: data.totalPayment,
            totalIntereses: data.totalInterest,
            tasa: data.monthlyInterestRate, // El backend retorna la tasa mensual
            loading: false
          });
        }
      } catch (err) {
        setCreditoResult(prev => ({ ...prev, loading: false }));
      }
    };
    
    const timeout = setTimeout(fetchSimulation, 300); // debounce
    return () => clearTimeout(timeout);
  }, [monto, plazo, tab]);

  return (
    <section id="simulador" className={`section ${styles.sim}`}>
      <div className={styles.bgGlow} />
      <div className="container">
        <div className={styles.header}>
          <span className="section-label">Simuladores</span>
          <h2 className="section-title">
            Calcula antes de decidir
          </h2>
          <p className="section-subtitle">
            Usa nuestras herramientas para entender cuánto pagarías de cuota o cuánto ganarías ahorrando con MET.
          </p>
        </div>

        <div className={styles.wrapper}>
          {/* Tabs */}
          <div className={styles.tabs}>
            <button className={`${styles.tab} ${tab === 'credito' ? styles.active : ''}`} onClick={() => setTab('credito')}>
              💳 Simulador de Crédito
            </button>
            <button className={`${styles.tab} ${tab === 'ahorro' ? styles.active : ''}`} onClick={() => setTab('ahorro')}>
              💰 Simulador de Ahorro
            </button>
          </div>

          {tab === 'credito' ? (
            <div className={styles.form}>
              <div className={styles.field}>
                <label className={styles.label}>
                  Monto del crédito
                  <span className={styles.value}>{formatCOP(monto)}</span>
                </label>
                <input type="range" min={500000} max={50000000} step={500000}
                  value={monto} onChange={(e) => setMonto(+e.target.value)} className={styles.range} />
                <div className={styles.rangeLabels}><span>{formatCOP(500000)}</span><span>{formatCOP(50000000)}</span></div>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>
                  Plazo
                  <span className={styles.value}>{plazo} meses</span>
                </label>
                <input type="range" min={6} max={60} step={6}
                  value={plazo} onChange={(e) => setPlazo(+e.target.value)} className={styles.range} />
                <div className={styles.rangeLabels}><span>6 meses</span><span>60 meses</span></div>
              </div>

              <div className={styles.result}>
                <div className={styles.resultMain}>
                  <span className={styles.resultLabel}>Cuota mensual estimada</span>
                  <span className={styles.resultValue}>
                    {creditoResult.loading ? 'Calculando...' : formatCOP(Math.round(creditoResult.cuota))}
                  </span>
                </div>
                <div className={styles.resultDetails}>
                  <div className={styles.resultItem}>
                    <span>Total a pagar</span>
                    <strong>{creditoResult.loading ? '...' : formatCOP(Math.round(creditoResult.totalPagar))}</strong>
                  </div>
                  <div className={styles.resultItem}>
                    <span>Total intereses</span>
                    <strong style={{ color: '#F59E0B' }}>
                      {creditoResult.loading ? '...' : formatCOP(Math.round(creditoResult.totalIntereses))}
                    </strong>
                  </div>
                  <div className={styles.resultItem}>
                    <span>Tasa mensual</span>
                    <strong>{creditoResult.loading ? '...' : `${(creditoResult.tasa * 100).toFixed(2)}%`}</strong>
                  </div>
                </div>
                <p className={styles.disclaimer}>* Simulación referencial. La tasa final depende del estudio de crédito.</p>
              </div>
            </div>
          ) : (
            <div className={styles.form}>
              <div className={styles.field}>
                <label className={styles.label}>
                  Aporte mensual
                  <span className={styles.value}>{formatCOP(aporte)}</span>
                </label>
                <input type="range" min={50000} max={5000000} step={50000}
                  value={aporte} onChange={(e) => setAporte(+e.target.value)} className={styles.range} />
                <div className={styles.rangeLabels}><span>{formatCOP(50000)}</span><span>{formatCOP(5000000)}</span></div>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>
                  Período
                  <span className={styles.value}>{meses} meses</span>
                </label>
                <input type="range" min={3} max={36} step={3}
                  value={meses} onChange={(e) => setMeses(+e.target.value)} className={styles.range} />
                <div className={styles.rangeLabels}><span>3 meses</span><span>36 meses</span></div>
              </div>

              <div className={styles.result}>
                <div className={styles.resultMain}>
                  <span className={styles.resultLabel}>Total ahorrado + intereses</span>
                  <span className={styles.resultValue}>{formatCOP(Math.round(totalFinal))}</span>
                </div>
                <div className={styles.resultDetails}>
                  <div className={styles.resultItem}>
                    <span>Tu aporte total</span>
                    <strong>{formatCOP(totalAhorro)}</strong>
                  </div>
                  <div className={styles.resultItem}>
                    <span>Intereses ganados</span>
                    <strong style={{ color: 'var(--primary)' }}>{formatCOP(Math.round(interesesAhorro))}</strong>
                  </div>
                  <div className={styles.resultItem}>
                    <span>Tasa mensual</span>
                    <strong>{tasaAhorro}%</strong>
                  </div>
                </div>
                <p className={styles.disclaimer}>* Simulación referencial. Las tasas pueden variar.</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
