package com.insurance.dashboard.service;

import com.insurance.dashboard.model.PolicyHolder;
import com.insurance.dashboard.repository.PolicyHolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyHolderServiceTest {

    @Mock
    private PolicyHolderRepository policyHolderRepository;

    @InjectMocks
    private PolicyHolderService policyHolderService;

    private PolicyHolder holder;

    @BeforeEach
    void setUp() {
        holder = PolicyHolder.builder()
                .id(1L).firstName("John").lastName("Smith")
                .email("john@email.com").phone("555-1001")
                .address("123 Orchard Rd, Singapore").build();
    }

    @Test
    void getAllPolicyHolders_returnsAllHolders() {
        when(policyHolderRepository.findAll()).thenReturn(List.of(holder));

        List<PolicyHolder> result = policyHolderService.getAllPolicyHolders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    void getPolicyHolderById_returnsHolder_whenFound() {
        when(policyHolderRepository.findById(1L)).thenReturn(Optional.of(holder));

        PolicyHolder result = policyHolderService.getPolicyHolderById(1L);

        assertThat(result.getEmail()).isEqualTo("john@email.com");
    }

    @Test
    void getPolicyHolderById_throwsException_whenNotFound() {
        when(policyHolderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyHolderService.getPolicyHolderById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PolicyHolder not found with id: 99");
    }

    @Test
    void createPolicyHolder_savesAndReturnsHolder() {
        when(policyHolderRepository.save(holder)).thenReturn(holder);

        PolicyHolder result = policyHolderService.createPolicyHolder(holder);

        assertThat(result.getLastName()).isEqualTo("Smith");
        verify(policyHolderRepository).save(holder);
    }

    @Test
    void updatePolicyHolder_updatesFieldsAndSaves() {
        PolicyHolder updated = PolicyHolder.builder()
                .firstName("Johnny").lastName("Smith")
                .email("johnny@email.com").phone("555-9999")
                .address("456 New Rd, Singapore").build();

        when(policyHolderRepository.findById(1L)).thenReturn(Optional.of(holder));
        when(policyHolderRepository.save(any(PolicyHolder.class))).thenReturn(holder);

        PolicyHolder result = policyHolderService.updatePolicyHolder(1L, updated);

        assertThat(result.getFirstName()).isEqualTo("Johnny");
        assertThat(result.getEmail()).isEqualTo("johnny@email.com");
        assertThat(result.getPhone()).isEqualTo("555-9999");
        verify(policyHolderRepository).save(holder);
    }

    @Test
    void deletePolicyHolder_callsRepositoryDelete() {
        doNothing().when(policyHolderRepository).deleteById(1L);

        policyHolderService.deletePolicyHolder(1L);

        verify(policyHolderRepository).deleteById(1L);
    }

    @Test
    void searchByLastName_returnsMatchingHolders() {
        when(policyHolderRepository.findByLastNameContainingIgnoreCase("Smith"))
                .thenReturn(List.of(holder));

        List<PolicyHolder> result = policyHolderService.searchByLastName("Smith");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualTo("Smith");
    }

    @Test
    void searchByLastName_returnsEmpty_whenNoMatch() {
        when(policyHolderRepository.findByLastNameContainingIgnoreCase("Unknown"))
                .thenReturn(List.of());

        List<PolicyHolder> result = policyHolderService.searchByLastName("Unknown");

        assertThat(result).isEmpty();
    }
}
