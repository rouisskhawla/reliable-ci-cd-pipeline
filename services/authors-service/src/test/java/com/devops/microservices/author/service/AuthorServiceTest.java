package com.devops.microservices.author.service;

import com.devops.microservices.author.model.Author;
import com.devops.microservices.author.repository.AuthorRepository;
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
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepo;

    @InjectMocks
    private AuthorService authorService;

    private Author author;

    @BeforeEach
    void setUp() {
        author = new Author(1L, "George Orwell", "1903-06-25", "British");
    }


    @Test
    void getAllAuthors_returnsListFromRepository() {
        when(authorRepo.findAll()).thenReturn(List.of(author));

        List<Author> result = authorService.getAllAuthors();

        assertThat(result).hasSize(1).containsExactly(author);
        verify(authorRepo).findAll();
    }

    @Test
    void getAllAuthors_returnsEmptyListWhenRepositoryThrows() {
        when(authorRepo.findAll()).thenThrow(new RuntimeException("DB error"));

        List<Author> result = authorService.getAllAuthors();

        assertThat(result).isEmpty();
    }


    @Test
    void addAuthor_savesAndReturnsAuthor() {
        when(authorRepo.save(author)).thenReturn(author);

        Author result = authorService.addAuthor(author);

        assertThat(result).isEqualTo(author);
        verify(authorRepo).save(author);
    }

    @Test
    void addAuthor_returnsNullWhenRepositoryThrows() {
        when(authorRepo.save(any())).thenThrow(new RuntimeException("Constraint violation"));

        Author result = authorService.addAuthor(author);

        assertThat(result).isNull();
    }


    @Test
    void updateAuthor_updatesAndReturnsAuthorWhenFound() {
        Author updated = new Author(1L, "Eric Blair", "1903-06-25", "British");
        when(authorRepo.existsById(1L)).thenReturn(true);
        when(authorRepo.save(any(Author.class))).thenReturn(updated);

        Author result = authorService.updateAuthor(1L, updated);

        assertThat(result.getName()).isEqualTo("Eric Blair");
        verify(authorRepo).save(updated);
    }

    @Test
    void updateAuthor_returnsNullWhenAuthorNotFound() {
        when(authorRepo.existsById(99L)).thenReturn(false);

        Author result = authorService.updateAuthor(99L, author);

        assertThat(result).isNull();
        verify(authorRepo, never()).save(any());
    }

    @Test
    void updateAuthor_setsIdOnAuthorBeforeSaving() {
        Author incoming = new Author(null, "Eric Blair", "1903-06-25", "British");
        when(authorRepo.existsById(1L)).thenReturn(true);
        when(authorRepo.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

        Author result = authorService.updateAuthor(1L, incoming);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void updateAuthor_returnsNullWhenRepositoryThrows() {
        when(authorRepo.existsById(1L)).thenReturn(true);
        when(authorRepo.save(any())).thenThrow(new RuntimeException("DB error"));

        Author result = authorService.updateAuthor(1L, author);

        assertThat(result).isNull();
    }


    @Test
    void deleteAuthor_returnsTrueWhenAuthorExists() {
        when(authorRepo.existsById(1L)).thenReturn(true);
        doNothing().when(authorRepo).deleteById(1L);

        boolean result = authorService.deleteAuthor(1L);

        assertThat(result).isTrue();
        verify(authorRepo).deleteById(1L);
    }

    @Test
    void deleteAuthor_returnsFalseWhenAuthorNotFound() {
        when(authorRepo.existsById(99L)).thenReturn(false);

        boolean result = authorService.deleteAuthor(99L);

        assertThat(result).isFalse();
        verify(authorRepo, never()).deleteById(any());
    }

    @Test
    void deleteAuthor_returnsFalseWhenRepositoryThrows() {
        when(authorRepo.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(authorRepo).deleteById(1L);

        boolean result = authorService.deleteAuthor(1L);

        assertThat(result).isFalse();
    }
}
