import React from 'react';
import { render, screen } from '@testing-library/react';
import StatsCard from '@/components/dashboard/StatsCard';

describe('StatsCard', () => {
  it('제목과 숫자 값을 렌더링한다', () => {
    render(<StatsCard title="총 신호등 수" value={12345} />);

    expect(screen.getByText('총 신호등 수')).toBeInTheDocument();
    expect(screen.getByText('12,345')).toBeInTheDocument();
  });

  it('문자열 값을 렌더링한다', () => {
    render(<StatsCard title="상태" value="활성" />);

    expect(screen.getByText('상태')).toBeInTheDocument();
    expect(screen.getByText('활성')).toBeInTheDocument();
  });

  it('아이콘이 제공되면 표시한다', () => {
    render(<StatsCard title="테스트" value={0} icon="test-icon" />);

    expect(screen.getByText('test-icon')).toBeInTheDocument();
  });

  it('아이콘이 없으면 아이콘 영역이 없다', () => {
    const { container } = render(<StatsCard title="테스트" value={0} />);

    expect(container.querySelector('.text-3xl')).not.toBeInTheDocument();
  });

  it('0 값을 올바르게 표시한다', () => {
    render(<StatsCard title="실패" value={0} />);

    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('큰 숫자에 천 단위 구분자가 적용된다', () => {
    render(<StatsCard title="대량" value={1234567} />);

    expect(screen.getByText('1,234,567')).toBeInTheDocument();
  });
});
