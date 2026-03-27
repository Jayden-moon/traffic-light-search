'use client';

import { useState, useCallback } from 'react';
import SearchForm from '@/components/search/SearchForm';
import FilterPanel, { Filters } from '@/components/search/FilterPanel';
import ResultsTable from '@/components/search/ResultsTable';
import { searchTrafficLights } from '@/lib/api';
import { SearchResponse } from '@/types/trafficLight';

export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<Filters>({});
  const [results, setResults] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const doSearch = useCallback(
    async (q: string, f: Filters, page: number = 0) => {
      setLoading(true);
      setError(null);
      try {
        const data = await searchTrafficLights({
          query: q || undefined,
          ...f,
          page,
          size: 20,
        });
        setResults(data);
      } catch (err: unknown) {
        const message =
          err instanceof Error
            ? err.message
            : 'API 서버에 연결할 수 없습니다.';
        setError(message);
        setResults(null);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  function handleSearch(newQuery: string) {
    setQuery(newQuery);
    doSearch(newQuery, filters, 0);
  }

  function handleFilterChange(newFilters: Filters) {
    setFilters(newFilters);
    doSearch(query, newFilters, 0);
  }

  function handlePageChange(page: number) {
    doSearch(query, filters, page);
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Search Form */}
      <div className="mb-6">
        <SearchForm onSearch={handleSearch} />
      </div>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
          {error}
        </div>
      )}

      {/* Content: Filter + Results */}
      <div className="flex flex-col lg:flex-row gap-6">
        {/* Filter Panel */}
        <div className="w-full lg:w-64 flex-shrink-0">
          <FilterPanel filters={filters} onFilterChange={handleFilterChange} />
        </div>

        {/* Results Table */}
        <div className="flex-1 min-w-0">
          <ResultsTable
            results={results}
            onPageChange={handlePageChange}
            loading={loading}
          />
        </div>
      </div>
    </div>
  );
}
