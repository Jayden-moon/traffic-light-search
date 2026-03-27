'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState, useEffect } from 'react';
import { createIndex, loadData, getIndexStatus } from '@/lib/api';
import { IndexStatus } from '@/types/trafficLight';

export default function Header() {
  const pathname = usePathname();
  const [indexing, setIndexing] = useState(false);
  const [indexStatus, setIndexStatus] = useState<IndexStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const navLinks = [
    { href: '/search', label: '검색' },
    { href: '/map', label: '지도' },
    { href: '/dashboard', label: '대시보드' },
  ];

  useEffect(() => {
    fetchStatus();
  }, []);

  async function fetchStatus() {
    try {
      const status = await getIndexStatus();
      setIndexStatus(status);
    } catch {
      // API might not be running
    }
  }

  async function handleIndexing() {
    setIndexing(true);
    setError(null);
    try {
      await createIndex();
      const status = await loadData();
      setIndexStatus(status);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : '인덱싱 중 오류가 발생했습니다.';
      setError(message);
    } finally {
      setIndexing(false);
    }
  }

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-gray-900 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center space-x-8">
            <Link href="/search" className="text-xl font-bold text-blue-400">
              신호등 검색
            </Link>

            {/* Nav links */}
            <nav className="hidden md:flex space-x-1">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    pathname === link.href
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-300 hover:bg-gray-700 hover:text-white'
                  }`}
                >
                  {link.label}
                </Link>
              ))}
            </nav>
          </div>

          {/* Right side */}
          <div className="flex items-center space-x-4">
            {indexStatus && indexStatus.indexExists && (
              <span className="hidden sm:inline text-sm text-gray-400">
                문서 수: {indexStatus.indexed?.toLocaleString() ?? '—'}
              </span>
            )}

            {error && (
              <span className="text-xs text-red-400 max-w-[200px] truncate">
                {error}
              </span>
            )}

            <button
              onClick={handleIndexing}
              disabled={indexing}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                indexing
                  ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700 text-white'
              }`}
            >
              {indexing ? '인덱싱 중...' : '데이터 인덱싱'}
            </button>
          </div>
        </div>

        {/* Mobile nav */}
        <nav className="md:hidden pb-3 flex space-x-1">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                pathname === link.href
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-300 hover:bg-gray-700 hover:text-white'
              }`}
            >
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
