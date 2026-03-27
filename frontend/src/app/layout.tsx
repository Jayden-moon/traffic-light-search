import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import Header from '@/components/layout/Header';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: '전국 신호등 검색 시스템',
  description: '전국 신호등 정보를 검색하고 지도에서 확인하세요.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className={inter.className}>
        <Header />
        <main className="pt-20 min-h-screen bg-gray-50">{children}</main>
      </body>
    </html>
  );
}
