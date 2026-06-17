package com.devops.microservices.books.service;

import com.devops.microservices.books.model.Book;
import com.devops.microservices.books.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepo;

    @InjectMocks
    private BookService bookService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book(1L, "1984", "George Orwell", 1949);
    }


    @Test
    void getAllBooks_returnsListFromRepository() {
        when(bookRepo.findAll()).thenReturn(List.of(book));

        List<Book> result = bookService.getAllBooks();

        assertThat(result).hasSize(1).containsExactly(book);
        verify(bookRepo).findAll();
    }

    @Test
    void getAllBooks_returnsEmptyListWhenRepositoryThrows() {
        when(bookRepo.findAll()).thenThrow(new RuntimeException("DB error"));

        List<Book> result = bookService.getAllBooks();

        assertThat(result).isEmpty();
    }


    @Test
    void addBook_savesAndReturnsBook() {
        when(bookRepo.save(book)).thenReturn(book);

        Book result = bookService.addBook(book);

        assertThat(result).isEqualTo(book);
        verify(bookRepo).save(book);
    }

    @Test
    void addBook_returnsNullWhenRepositoryThrows() {
        when(bookRepo.save(any())).thenThrow(new RuntimeException("Constraint violation"));

        Book result = bookService.addBook(book);

        assertThat(result).isNull();
    }


    @Test
    void updateBook_updatesAndReturnsBookWhenFound() {
        Book updated = new Book(1L, "Animal Farm", "George Orwell", 1945);
        when(bookRepo.existsById(1L)).thenReturn(true);
        when(bookRepo.save(any(Book.class))).thenReturn(updated);

        Book result = bookService.updateBook(1L, updated);

        assertThat(result.getTitle()).isEqualTo("Animal Farm");
        verify(bookRepo).save(updated);
    }

    @Test
    void updateBook_returnsNullWhenBookNotFound() {
        when(bookRepo.existsById(99L)).thenReturn(false);

        Book result = bookService.updateBook(99L, book);

        assertThat(result).isNull();
        verify(bookRepo, never()).save(any());
    }

    @Test
    void updateBook_setsIdOnBookBeforeSaving() {
        Book incoming = new Book(null, "Animal Farm", "George Orwell", 1945);
        when(bookRepo.existsById(1L)).thenReturn(true);
        when(bookRepo.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.updateBook(1L, incoming);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void updateBook_returnsNullWhenRepositoryThrows() {
        when(bookRepo.existsById(1L)).thenReturn(true);
        when(bookRepo.save(any())).thenThrow(new RuntimeException("DB error"));

        Book result = bookService.updateBook(1L, book);

        assertThat(result).isNull();
    }


    @Test
    void deleteBook_returnsTrueWhenBookExists() {
        when(bookRepo.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepo).deleteById(1L);

        boolean result = bookService.deleteBook(1L);

        assertThat(result).isTrue();
        verify(bookRepo).deleteById(1L);
    }

    @Test
    void deleteBook_returnsFalseWhenBookNotFound() {
        when(bookRepo.existsById(99L)).thenReturn(false);

        boolean result = bookService.deleteBook(99L);

        assertThat(result).isFalse();
        verify(bookRepo, never()).deleteById(any());
    }

    @Test
    void deleteBook_returnsFalseWhenRepositoryThrows() {
        when(bookRepo.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(bookRepo).deleteById(1L);

        boolean result = bookService.deleteBook(1L);

        assertThat(result).isFalse();
    }
}
