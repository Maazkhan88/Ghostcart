import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ghost Cart — Add to cart. Checkout. Keep your money.",
  description:
    "Ghost Cart is the simulation-only fake checkout experience for everything you almost bought. No real payment. No real delivery.",
  keywords: ["Ghost Cart", "almost bought", "impulse shopping", "fake checkout", "shopping simulation"],
  openGraph: {
    title: "Ghost Cart",
    description: "The fake checkout experience for everything you almost bought.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
