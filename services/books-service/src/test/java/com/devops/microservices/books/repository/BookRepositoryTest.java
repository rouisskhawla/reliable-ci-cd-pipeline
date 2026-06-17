package com.devops.microservices.books.repository;

import com.devops.microservices.books.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void save_persistsBookAndAssignsId() {
        Book book = new Book(null, "1984", "George Orwell", 1949);

        Book saved = bookRepository.save(book);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("1984");
    }

    @Test
    void findAll_returnsAllPersistedBooks() {
        bookRepository.save(new Book(null, "1984", "George Orwell", 1949));
        bookRepository.save(new Book(null, "Brave New World", "Aldous Huxley", 1932));

        List<Book> books = bookRepository.findAll();

        assertThat(books).hasSize(2);
    }

    @Test
    void existsById_returnsTrueForPersistedBook() {
        Book saved = bookRepository.save(new Book(null, "1984", "George Orwell", 1949));

        assertThat(bookRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_returnsFalseForNonExistentId() {
        assertThat(bookRepository.existsById(999L)).isFalse();
    }

    @Test
    void deleteById_removesBookFromDatabase() {
        Book saved = bookRepository.save(new Book(null, "1984", "George Orwell", 1949));
        Long id = saved.getId();

        bookRepository.deleteById(id);

        assertThat(bookRepository.findById(id)).isEmpty();
    }

    @Test
    void save_updatesExistingBookWhenIdIsSet() {
        Book saved = bookRepository.save(new Book(null, "1984", "George Orwell", 1949));
        saved.setTitle("Animal Farm");

        Book updated = bookRepository.save(saved);

        assertThat(updated.getTitle()).isEqualTo("Animal Farm");
        assertThat(bookRepository.findAll()).hasSize(1);
    }

    @Test
    void findById_returnsBookWhenExists() {
        Book saved = bookRepository.save(new Book(null, "1984", "George Orwell", 1949));

        Optional<Book> found = bookRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getYear()).isEqualTo(1949);
    }
}
