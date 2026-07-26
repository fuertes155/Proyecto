import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Política de Privacidad — MET Cooperativa',
  description:
    'Conozca cómo MET Cooperativa recopila, usa y protege sus datos personales conforme a la Ley 1581 de 2012 (Habeas Data) y el RGPD.',
};

export default function PrivacidadPage() {
  return (
    <main className="max-w-3xl mx-auto px-6 py-16">
      <h1 className="text-3xl font-bold mb-2">Política de Privacidad</h1>
      <p className="text-sm text-gray-500 mb-8">
        Última actualización: julio de 2025
      </p>

      <section className="prose prose-gray max-w-none space-y-8">
        <div>
          <h2>1. Responsable del tratamiento</h2>
          <p>
            <strong>MET Cooperativa Financiera</strong> (en adelante, &quot;MET&quot;),
            identificada con NIT [número], con domicilio en [ciudad], Colombia,
            es responsable del tratamiento de sus datos personales en los términos
            de la Ley 1581 de 2012 y el Decreto 1377 de 2013.
          </p>
        </div>

        <div>
          <h2>2. Datos que recopilamos</h2>
          <ul>
            <li><strong>Datos de identidad</strong>: nombre, apellido, número de documento, fecha de nacimiento.</li>
            <li><strong>Datos de contacto</strong>: correo electrónico, número de teléfono.</li>
            <li><strong>Datos financieros</strong>: información de transacciones, saldos, historial crediticio (consultado con su consentimiento explícito de Habeas Data).</li>
            <li><strong>Datos biométricos</strong>: huellas o reconocimiento facial utilizados para autenticación segura (solo en la app móvil).</li>
            <li><strong>Datos de dispositivo</strong>: dirección IP, identificador de dispositivo, sistema operativo (para prevención de fraude).</li>
          </ul>
        </div>

        <div>
          <h2>3. Finalidades del tratamiento</h2>
          <ul>
            <li>Prestación de servicios financieros cooperativos (ahorro, crédito, inversión).</li>
            <li>Cumplimiento de obligaciones regulatorias (SARLAFT, reportes a la Superintendencia de la Economía Solidaria).</li>
            <li>Prevención de fraude y lavado de activos.</li>
            <li>Comunicaciones comerciales y de servicio (con su consentimiento).</li>
            <li>Mejora de nuestros productos y servicios.</li>
          </ul>
        </div>

        <div>
          <h2>4. Seguridad de sus datos</h2>
          <p>
            MET implementa medidas técnicas y organizativas de alto nivel para proteger
            sus datos, incluyendo: cifrado AES-256 en reposo, TLS 1.3 en tránsito,
            autenticación multifactor, y auditoría continua de accesos.
          </p>
        </div>

        <div>
          <h2>5. Sus derechos (Habeas Data)</h2>
          <p>
            De conformidad con la Ley 1581 de 2012, usted tiene derecho a:
          </p>
          <ul>
            <li><strong>Conocer</strong> los datos que tenemos sobre usted.</li>
            <li><strong>Actualizar</strong> y rectificar datos incorrectos.</li>
            <li><strong>Suprimir</strong> datos cuando no sean necesarios para las finalidades declaradas.</li>
            <li><strong>Revocar</strong> el consentimiento para tratamientos opcionales.</li>
          </ul>
          <p>
            Para ejercer estos derechos, contáctenos en:{' '}
            <a href="mailto:privacidad@cooperativa.met.com">
              privacidad@cooperativa.met.com
            </a>
          </p>
        </div>

        <div>
          <h2>6. Transferencias internacionales</h2>
          <p>
            Sus datos pueden ser procesados en servidores ubicados en los Estados Unidos
            bajo garantías contractuales adecuadas. No vendemos ni compartimos sus datos
            con terceros para fines publicitarios.
          </p>
        </div>

        <div>
          <h2>7. Cambios a esta política</h2>
          <p>
            Notificaremos cambios materiales con al menos 10 días de anticipación
            a través de la aplicación y/o correo electrónico registrado.
          </p>
        </div>

        <div>
          <h2>8. Contacto</h2>
          <p>
            Para consultas sobre privacidad, escríbanos a{' '}
            <a href="mailto:privacidad@cooperativa.met.com">
              privacidad@cooperativa.met.com
            </a>{' '}
            o llámenos al [número de contacto].
          </p>
        </div>
      </section>
    </main>
  );
}
