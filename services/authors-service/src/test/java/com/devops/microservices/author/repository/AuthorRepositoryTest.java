package com.devops.microservices.author.repository;

import com.devops.microservices.author.model.Author;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void save_persistsAuthorAndAssignsId() {
        Author author = new Author(null, "George Orwell", "1903-06-25", "British");

        Author saved = authorRepository.save(author);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("George Orwell");
    }

    @Test
    void findAll_returnsAllPersistedAuthors() {
        authorRepository.save(new Author(null, "George Orwell", "1903-06-25", "British"));
        authorRepository.save(new Author(null, "Aldous Huxley", "1894-07-26", "British"));

        List<Author> authors = authorRepository.findAll();

        assertThat(authors).hasSize(2);
    }

    @Test
    void existsById_returnsTrueForPersistedAuthor() {
        Author saved = authorRepository.save(new Author(null, "George Orwell", "1903-06-25", "British"));

        assertThat(authorRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_returnsFalseForNonExistentId() {
        assertThat(authorRepository.existsById(999L)).isFalse();
    }

    @Test
    void deleteById_removesAuthorFromDatabase() {
        Author saved = authorRepository.save(new Author(null, "George Orwell", "1903-06-25", "British"));
        Long id = saved.getId();

        authorRepository.deleteById(id);

        assertThat(authorRepository.findById(id)).isEmpty();
    }

    @Test
    void save_updatesExistingAuthorWhenIdIsSet() {
        Author saved = authorRepository.save(new Author(null, "George Orwell", "1903-06-25", "British"));
        saved.setName("Eric Blair");

        Author updated = authorRepository.save(saved);

        assertThat(updated.getName()).isEqualTo("Eric Blair");
        assertThat(authorRepository.findAll()).hasSize(1);
    }

    @Test
    void findById_returnsAuthorWhenExists() {
        Author saved = authorRepository.save(new Author(null, "George Orwell", "1903-06-25", "British"));

        Optional<Author> found = authorRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNationality()).isEqualTo("British");
    }
}
