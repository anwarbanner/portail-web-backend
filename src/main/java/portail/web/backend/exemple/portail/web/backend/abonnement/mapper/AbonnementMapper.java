package portail.web.backend.exemple.portail.web.backend.abonnement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.MonAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;
import portail.web.backend.exemple.portail.web.backend.consultation.dto.ConsultationResponse;
import portail.web.backend.exemple.portail.web.backend.consultation.entity.Consultation;

@Mapper(componentModel = "spring")
public interface AbonnementMapper {

    PlanAbonnementResponse toPlanResponse(PlanAbonnement plan);

    @Mapping(target = "userId",    source = "user.id")
    @Mapping(target = "username",  source = "user.username")
    @Mapping(target = "planId",    source = "plan.id")
    @Mapping(target = "planNom",   source = "plan.nom")
    @Mapping(target = "illimite",  expression = "java(abonnement.getPlan().isIllimite())")
    @Mapping(target = "actif",     expression = "java(abonnement.isActif())")
    @Mapping(target = "statut",    expression = "java(abonnement.getStatut().name())")
    AbonnementResponse toResponse(Abonnement abonnement);

    @Mapping(target = "planNom",             source = "plan.nom")
    @Mapping(target = "planDescription",     source = "plan.description")
    @Mapping(target = "prix",                source = "plan.prix")
    @Mapping(target = "dureeMois",           source = "plan.dureeMois")
    @Mapping(target = "nombreConsultations", source = "plan.nombreConsultations")
    @Mapping(target = "illimite",            expression = "java(abonnement.getPlan().isIllimite())")
    @Mapping(target = "actif",               expression = "java(abonnement.isActif())")
    @Mapping(target = "statut",              expression = "java(abonnement.getStatut().name())")
    MonAbonnementResponse toMonResponse(Abonnement abonnement);

    @Mapping(target = "normeId",        source = "norme.id")
    @Mapping(target = "normeReference", source = "norme.reference")
    @Mapping(target = "normeTitreFr",   source = "norme.titreFr")
    ConsultationResponse toConsultationResponse(Consultation consultation);
}
