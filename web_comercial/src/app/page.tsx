import Navbar from '@/components/Navbar';
import HeroSection from '@/components/HeroSection';
import StatsSection from '@/components/StatsSection';
import ProductsSection from '@/components/ProductsSection';
import SimulatorSection from '@/components/SimulatorSection';
import AboutSection from '@/components/AboutSection';
import BlogSection from '@/components/BlogSection';
import FAQSection from '@/components/FAQSection';
import ContactSection from '@/components/ContactSection';
import Footer from '@/components/Footer';

export default function Home() {
  return (
    <main>
      <Navbar />
      <HeroSection />
      <StatsSection />
      <ProductsSection />
      <SimulatorSection />
      <AboutSection />
      <BlogSection />
      <FAQSection />
      <ContactSection />
      <Footer />
    </main>
  );
}
