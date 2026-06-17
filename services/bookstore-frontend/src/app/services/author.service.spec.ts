import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuthorsService } from './author.service';
import { of, throwError } from 'rxjs';
import { Author } from '../models/author.model';

const mockHttp = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
};

describe('AuthorsService', () => {
  let service: AuthorsService;

  const mockAuthor: Author = {
    id: 1,
    name: 'George Orwell',
    date_of_birth: '1903-06-25',
    nationality: 'British',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    service = new AuthorsService(mockHttp as any);
  });


  it('getAllAuthors: calls GET /authors and returns observable of authors', () => {
    mockHttp.get.mockReturnValue(of([mockAuthor]));

    service.getAllAuthors().subscribe((authors) => {
      expect(authors).toHaveLength(1);
      expect(authors[0].name).toBe('George Orwell');
    });

    expect(mockHttp.get).toHaveBeenCalledWith(expect.stringContaining('/authors'));
  });

  it('getAllAuthors: returns empty array when no authors exist', () => {
    mockHttp.get.mockReturnValue(of([]));

    service.getAllAuthors().subscribe((authors) => {
      expect(authors).toHaveLength(0);
    });
  });


  it('addAuthor: calls POST /authors with the author payload', () => {
    mockHttp.post.mockReturnValue(of(mockAuthor));
    const newAuthor: Author = { name: 'George Orwell', date_of_birth: '1903-06-25', nationality: 'British' };

    service.addAuthor(newAuthor).subscribe((result) => {
      expect(result.id).toBe(1);
    });

    expect(mockHttp.post).toHaveBeenCalledWith(
      expect.stringContaining('/authors'),
      newAuthor,
    );
  });


  it('updateAuthor: calls PUT /authors/:id with the author payload', () => {
    const updated: Author = { id: 1, name: 'Eric Blair', date_of_birth: '1903-06-25', nationality: 'British' };
    mockHttp.put.mockReturnValue(of(updated));

    service.updateAuthor(1, updated).subscribe((result) => {
      expect(result.name).toBe('Eric Blair');
    });

    expect(mockHttp.put).toHaveBeenCalledWith(
      expect.stringContaining('/authors/1'),
      updated,
    );
  });


  it('deleteAuthor: calls DELETE /authors/:id', () => {
    mockHttp.delete.mockReturnValue(of(undefined));

    service.deleteAuthor(1).subscribe();

    expect(mockHttp.delete).toHaveBeenCalledWith(expect.stringContaining('/authors/1'));
  });

  it('deleteAuthor: propagates error when API fails', () => {
    mockHttp.delete.mockReturnValue(throwError(() => new Error('Not Found')));
    const errorSpy = vi.fn();

    service.deleteAuthor(99).subscribe({ error: errorSpy });

    expect(errorSpy).toHaveBeenCalled();
  });
});
