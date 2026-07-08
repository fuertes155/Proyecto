import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "MET Cooperativa | Tu dinero, construyendo futuro",
  description: "MET es la cooperativa financiera digital que te ofrece cuentas, créditos, inversiones y transferencias con la tecnología más segura. Únete hoy.",
  keywords: "cooperativa, finanzas, créditos, inversiones, transferencias, ahorro, MET",
  authors: [{ name: "MET Cooperativa" }],
  openGraph: {
    title: "MET Cooperativa | Tu dinero, construyendo futuro",
    description: "Cuentas, créditos, inversiones y transferencias digitales seguras.",
    type: "website",
    locale: "es_CO",
  },
  robots: "index, follow",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
