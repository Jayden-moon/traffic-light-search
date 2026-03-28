import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchForm from '@/components/search/SearchForm';

describe('SearchForm', () => {
  it('검색 입력창이 렌더링된다', () => {
    render(<SearchForm onSearch={jest.fn()} />);

    expect(
      screen.getByPlaceholderText('주소, 도로명, 관리기관 등으로 검색...')
    ).toBeInTheDocument();
  });

  it('검색 버튼이 렌더링된다', () => {
    render(<SearchForm onSearch={jest.fn()} />);

    expect(screen.getByRole('button', { name: '검색' })).toBeInTheDocument();
  });

  it('폼 제출 시 onSearch가 검색어와 함께 호출된다', async () => {
    const onSearch = jest.fn();
    render(<SearchForm onSearch={onSearch} />);

    const input = screen.getByPlaceholderText('주소, 도로명, 관리기관 등으로 검색...');
    await userEvent.type(input, '강남구');

    fireEvent.submit(input.closest('form')!);

    expect(onSearch).toHaveBeenCalledWith('강남구');
  });

  it('빈 검색어로도 제출 가능하다', () => {
    const onSearch = jest.fn();
    render(<SearchForm onSearch={onSearch} />);

    fireEvent.submit(screen.getByRole('button', { name: '검색' }).closest('form')!);

    expect(onSearch).toHaveBeenCalledWith('');
  });

  it('검색 버튼 클릭으로 폼이 제출된다', async () => {
    const onSearch = jest.fn();
    render(<SearchForm onSearch={onSearch} />);

    const input = screen.getByPlaceholderText('주소, 도로명, 관리기관 등으로 검색...');
    await userEvent.type(input, '테헤란로');
    await userEvent.click(screen.getByRole('button', { name: '검색' }));

    expect(onSearch).toHaveBeenCalledWith('테헤란로');
  });
});
