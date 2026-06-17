package com.devops.microservices.author.controller;

import com.devops.microservices.author.model.Author;
import com.devops.microservices.author.service.AuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorService authorService;

    @Autowired
    private ObjectMapper objectMapper;

    private Author author;

    @BeforeEach
    void setUp() {
        author = new Author(1L, "George Orwell", "1903-06-25", "British");
    }


    @Test
    void getAllAuthors_returns200WithAuthorList() throws Exception {
        when(authorService.getAllAuthors()).thenReturn(List.of(author));

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("George Orwell"))
                .andExpect(jsonPath("$[0].nationality").value("British"));
    }

    @Test
    void getAllAuthors_returns200WithEmptyListWhenNoAuthors() throws Exception {
        when(authorService.getAllAuthors()).thenReturn(List.of());

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    void addAuthor_returns200WithCreatedAuthor() throws Exception {
        when(authorService.addAuthor(any(Author.class))).thenReturn(author);

        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("George Orwell"));
    }


    @Test
    void updateAuthor_returns200WithSuccessMessageWhenFound() throws Exception {
        Author updated = new Author(1L, "Eric Blair", "1903-06-25", "British");
        when(authorService.updateAuthor(eq(1L), any(Author.class))).thenReturn(updated);

        mockMvc.perform(put("/api/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Author updated successfully"))
                .andExpect(jsonPath("$.updatedAuthor.name").value("Eric Blair"));
    }

    @Test
    void updateAuthor_returns404WhenAuthorNotFound() throws Exception {
        when(authorService.updateAuthor(eq(99L), any(Author.class))).thenReturn(null);

        mockMvc.perform(put("/api/authors/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(author)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found or could not be updated"));
    }


    @Test
    void deleteAuthor_returns200WithSuccessMessageWhenFound() throws Exception {
        when(authorService.deleteAuthor(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Author deleted successfully"));
    }

    @Test
    void deleteAuthor_returns404WhenAuthorNotFound() throws Exception {
        when(authorService.deleteAuthor(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/authors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found or could not be deleted"));
    }
}
