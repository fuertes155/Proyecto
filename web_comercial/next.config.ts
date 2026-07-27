import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Necesario para el Dockerfile de producción — genera un servidor Node mínimo
  output: "standalone",
};

export default nextConfig;

