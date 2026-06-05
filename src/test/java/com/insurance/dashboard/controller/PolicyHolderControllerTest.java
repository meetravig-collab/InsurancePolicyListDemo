package com.insurance.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.dashboard.model.PolicyHolder;
import com.insurance.dashboard.service.PolicyHolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyHolderController.class)
class PolicyHolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyHolderService policyHolderService;

    private PolicyHolder holder;

    @BeforeEach
    void setUp() {
        holder = PolicyHolder.builder()
                .id(1L).firstName("John").lastName("Smith")
                .email("john@email.com").phone("555-1001")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .address("123 Orchard Rd, Singapore").build();
    }

    @Test
    void getAllPolicyHolders_returnsList() throws Exception {
        when(policyHolderService.getAllPolicyHolders()).thenReturn(List.of(holder));

        mockMvc.perform(get("/api/policy-holders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Smith"))
                .andExpect(jsonPath("$[0].email").value("john@email.com"));
    }

    @Test
    void getAllPolicyHolders_withLastName_searchesByLastName() throws Exception {
        when(policyHolderService.searchByLastName("Smith")).thenReturn(List.of(holder));

        mockMvc.perform(get("/api/policy-holders?lastName=Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastName").value("Smith"));

        verify(policyHolderService).searchByLastName("Smith");
        verify(policyHolderService, never()).getAllPolicyHolders();
    }

    @Test
    void getPolicyHolderById_returnsHolder_whenFound() throws Exception {
        when(policyHolderService.getPolicyHolderById(1L)).thenReturn(holder);

        mockMvc.perform(get("/api/policy-holders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john@email.com"));
    }

    @Test
    void getPolicyHolderById_returns404_whenNotFound() throws Exception {
        when(policyHolderService.getPolicyHolderById(99L))
                .thenThrow(new RuntimeException("PolicyHolder not found with id: 99"));

        mockMvc.perform(get("/api/policy-holders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("PolicyHolder not found with id: 99"));
    }

    @Test
    void createPolicyHolder_returns201_withCreatedHolder() throws Exception {
        when(policyHolderService.createPolicyHolder(any(PolicyHolder.class))).thenReturn(holder);

        mockMvc.perform(post("/api/policy-holders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holder)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    void updatePolicyHolder_returnsUpdatedHolder() throws Exception {
        PolicyHolder updated = PolicyHolder.builder()
                .id(1L).firstName("Johnny").lastName("Smith")
                .email("johnny@email.com").phone("555-9999")
                .address("456 New Rd, Singapore").build();

        when(policyHolderService.updatePolicyHolder(eq(1L), any(PolicyHolder.class))).thenReturn(updated);

        mockMvc.perform(put("/api/policy-holders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.email").value("johnny@email.com"));
    }

    @Test
    void deletePolicyHolder_returns204() throws Exception {
        doNothing().when(policyHolderService).deletePolicyHolder(1L);

        mockMvc.perform(delete("/api/policy-holders/1"))
                .andExpect(status().isNoContent());

        verify(policyHolderService).deletePolicyHolder(1L);
    }
}
