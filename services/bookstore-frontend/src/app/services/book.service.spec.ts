import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BooksService } from './book.service';
import { of, throwError } from 'rxjs';
import { Book } from '../models/book.model';

const mockHttp = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
};

describe('BooksService', () => {
  let service: BooksService;

  const mockBook: Book = { id: 1, title: '1984', author: 'George Orwell', year: 1949 };

  beforeEach(() => {
    vi.clearAllMocks();
    service = new BooksService(mockHttp as any);
  });


  it('getAllBooks: calls GET /books and returns observable of books', () => {
    mockHttp.get.mockReturnValue(of([mockBook]));

    service.getAllBooks().subscribe((books) => {
      expect(books).toHaveLength(1);
      expect(books[0].title).toBe('1984');
    });

    expect(mockHttp.get).toHaveBeenCalledWith(expect.stringContaining('/books'));
  });

  it('getAllBooks: returns empty array when API responds with none', () => {
    mockHttp.get.mockReturnValue(of([]));

    service.getAllBooks().subscribe((books) => {
      expect(books).toHaveLength(0);
    });
  });


  it('addBook: calls POST /books with the book payload', () => {
    mockHttp.post.mockReturnValue(of(mockBook));
    const newBook: Book = { title: '1984', author: 'George Orwell', year: 1949 };

    service.addBook(newBook).subscribe((result) => {
      expect(result.id).toBe(1);
    });

    expect(mockHttp.post).toHaveBeenCalledWith(
      expect.stringContaining('/books'),
      newBook,
    );
  });


  it('updateBook: calls PUT /books/:id with the book payload', () => {
    const updated: Book = { id: 1, title: 'Animal Farm', author: 'George Orwell', year: 1945 };
    mockHttp.put.mockReturnValue(of(updated));

    service.updateBook(1, updated).subscribe((result) => {
      expect(result.title).toBe('Animal Farm');
    });

    expect(mockHttp.put).toHaveBeenCalledWith(
      expect.stringContaining('/books/1'),
      updated,
    );
  });


  it('deleteBook: calls DELETE /books/:id', () => {
    mockHttp.delete.mockReturnValue(of(undefined));

    service.deleteBook(1).subscribe();

    expect(mockHttp.delete).toHaveBeenCalledWith(expect.stringContaining('/books/1'));
  });

  it('deleteBook: propagates error when API fails', () => {
    mockHttp.delete.mockReturnValue(throwError(() => new Error('Not Found')));
    const errorSpy = vi.fn();

    service.deleteBook(99).subscribe({ error: errorSpy });

    expect(errorSpy).toHaveBeenCalled();
  });
});
