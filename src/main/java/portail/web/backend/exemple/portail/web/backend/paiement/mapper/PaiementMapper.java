package portail.web.backend.exemple.portail.web.backend.paiement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementResponse;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.Paiement;

@Mapper(componentModel = "spring")
public interface PaiementMapper {

    @Mapping(target = "abonnementId",   source = "abonnement.id")
    @Mapping(target = "userId",         source = "user.id")
    @Mapping(target = "username",       source = "user.username")
    @Mapping(target = "methodePaiement",  expression = "java(paiement.getMethodePaiement().name())")
    @Mapping(target = "statutPaiement",   expression = "java(paiement.getStatutPaiement().name())")
    PaiementResponse toResponse(Paiement paiement);
}
