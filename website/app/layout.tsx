import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SubhuMusic — Your sound, your space",
  description: "Discover music and play it on the web with SubhuMusic.",
  applicationName: "SubhuMusic",
  keywords: ["SubhuMusic", "music", "music discovery", "web player"],
  openGraph: {
    title: "SubhuMusic — Your sound, your space",
    description: "A music space made for the browser.",
    type: "website"
  }
};

export const viewport: Viewport = {
  themeColor: "#0b0a10",
  colorScheme: "dark",
  width: "device-width",
  initialScale: 1
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
