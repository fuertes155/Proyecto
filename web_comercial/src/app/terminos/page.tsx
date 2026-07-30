import type { Metadata } from 'next';
import LegalLayout from '@/components/LegalLayout';

export const metadata: Metadata = {
  title: 'Términos y Condiciones — MET',
  description:
    'Lea los términos y condiciones que rigen el uso de los servicios financieros de MET.',
};

export default function TerminosPage() {
  return (
    <LegalLayout title="Términos y Condiciones" updated="julio de 2025">
      <section>
        <h2>1. Aceptación</h2>
        <p>
          Al usar los servicios de MET Financiera, usted acepta
          estos términos en su totalidad. Si no está de acuerdo, no debe usar
          nuestros servicios.
        </p>
      </section>

      <section>
        <h2>2. Servicios ofrecidos</h2>
        <p>MET ofrece los siguientes servicios financieros cooperativos:</p>
        <ul>
          <li>Billetera virtual y transferencias electrónicas</li>
          <li>Ahorro programado y fondos solidarios</li>
          <li>Crédito personal y microcréditos grupales</li>
          <li>Micro-inversiones fraccionadas</li>
        </ul>
      </section>

      <section>
        <h2>3. Elegibilidad</h2>
        <p>
          Para usar nuestros servicios debe ser mayor de 18 años, residente en
          Colombia, y completar el proceso de verificación de identidad (KYC)
          exigido por la regulación.
        </p>
      </section>

      <section>
        <h2>4. Responsabilidades del usuario</h2>
        <ul>
          <li>Mantener la confidencialidad de su PIN y credenciales de acceso.</li>
          <li>Notificar de inmediato cualquier acceso no autorizado a su cuenta.</li>
          <li>Proporcionar información veraz y actualizada.</li>
          <li>No usar los servicios para actividades ilícitas.</li>
        </ul>
      </section>

      <section>
        <h2>5. Tarifas y comisiones</h2>
        <p>
          Las tarifas aplicables están disponibles en la sección de tarifas de
          la aplicación. MET notificará cambios con al menos 30 días de anticipación.
        </p>
      </section>

      <section>
        <h2>6. Limitación de responsabilidad</h2>
        <p>
          MET no será responsable por pérdidas derivadas del uso no autorizado
          de su cuenta cuando usted no haya cumplido con las medidas de seguridad
          recomendadas.
        </p>
      </section>

      <section>
        <h2>7. Ley aplicable</h2>
        <p>
          Estos términos se rigen por las leyes de la República de Colombia.
          Cualquier disputa será resuelta ante los jueces competentes de [ciudad].
        </p>
      </section>

      <section>
        <h2>8. Contacto</h2>
        <p>
          Para consultas sobre estos términos:{' '}
          <a href="mailto:legal@met.com">legal@met.com</a>
        </p>
      </section>
    </LegalLayout>
  );
}
