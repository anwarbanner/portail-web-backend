package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import portail.web.backend.exemple.portail.web.backend.abonnement.mapper.AbonnementMapper;
import portail.web.backend.exemple.portail.web.backend.consultation.dto.ConsultationResponse;
import portail.web.backend.exemple.portail.web.backend.consultation.entity.Consultation;
import portail.web.backend.exemple.portail.web.backend.consultation.repository.ConsultationRepository;
import portail.web.backend.exemple.portail.web.backend.consultation.service.ConsultationService;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.user.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock ConsultationRepository consultationRepository;
    @Mock AbonnementMapper abonnementMapper;
    @InjectMocks ConsultationService consultationService;

    // ── enregistrer ───────────────────────────────────────────────────────────

    @Test
    void enregistrer_savesConsultationWithUserAndNorme() {
        User user = new User("alice", "pw", "ROLE_USER");
        user.setId(1L);
        Norme norme = new Norme();

        consultationService.enregistrer(user, norme);

        ArgumentCaptor<Consultation> captor = ArgumentCaptor.forClass(Consultation.class);
        verify(consultationRepository).save(captor.capture());
        Consultation saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getNorme()).isSameAs(norme);
        assertThat(saved.getDateConsultation()).isNotNull();
    }

    @Test
    void enregistrer_setsDateConsultationToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        consultationService.enregistrer(new User(), new Norme());
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        ArgumentCaptor<Consultation> captor = ArgumentCaptor.forClass(Consultation.class);
        verify(consultationRepository).save(captor.capture());
        LocalDateTime date = captor.getValue().getDateConsultation();
        assertThat(date).isAfter(before).isBefore(after);
    }

    // ── findByUser ────────────────────────────────────────────────────────────

    @Test
    void findByUser_returnsPageOfConsultationResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Consultation consultation = new Consultation();
        ConsultationResponse response = new ConsultationResponse(1L, 2L, "ISO-001", "Titre", LocalDateTime.now());

        when(consultationRepository.findByUserIdOrderByDateConsultationDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(consultation)));
        when(abonnementMapper.toConsultationResponse(consultation)).thenReturn(response);

        Page<ConsultationResponse> result = consultationService.findByUser(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).normeReference()).isEqualTo("ISO-001");
    }

    @Test
    void findByUser_emptyResult_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(consultationRepository.findByUserIdOrderByDateConsultationDesc(99L, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ConsultationResponse> result = consultationService.findByUser(99L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
