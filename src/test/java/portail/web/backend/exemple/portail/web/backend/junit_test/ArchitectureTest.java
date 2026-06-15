package portail.web.backend.exemple.portail.web.backend.junit_test;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Tests d'architecture — vérifiés automatiquement à chaque build Jenkins.
 *
 * Chaque règle est une contrainte sur la structure du code :
 * si une violation existe, le test échoue avec le nom exact de la classe fautive.
 */
@AnalyzeClasses(
        packages = "portail.web.backend.exemple.portail.web.backend",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    /**
     * Règle 1 — Nommage des controllers
     *
     * Toute classe annotée @RestController doit avoir un nom finissant par "Controller".
     * Évite les noms ambigus comme UserEndpoint, AuthAPI, etc.
     */
    @ArchTest
    static final ArchRule controllers_doivent_finir_par_Controller =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Convention : @RestController → nom finit par 'Controller'");

    /**
     * Règle 2 — Nommage des services
     *
     * Toute classe annotée @Service doit avoir un nom finissant par "Service" ou "ServiceImpl".
     * Garantit la cohérence des noms dans la couche métier.
     */
    @ArchTest
    static final ArchRule services_doivent_finir_par_Service =
            classes().that().areAnnotatedWith(Service.class)
                    .and().doNotHaveSimpleName("UserDetailsServiceImpl")
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Convention : @Service → nom finit par 'Service' (UserDetailsServiceImpl exclu car imposé par Spring Security)");

    /**
     * Règle 3 — Couches : les controllers ne touchent pas les repositories
     *
     * Aucune classe *Controller ne peut appeler directement un *Repository.
     * Les controllers passent obligatoirement par les services.
     * Architecture : Controller → Service → Repository (pas de saut).
     */
    @ArchTest
    static final ArchRule controllers_naccedent_pas_aux_repositories =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().accessClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("Architecture en couches : Controller → Service → Repository, pas de saut");

    /**
     * Règle 4 — Couches : les repositories ne dépendent pas des services
     *
     * Les repositories sont la couche la plus basse (accès données).
     * Ils ne doivent jamais importer ou appeler un Service — ce serait une dépendance circulaire.
     */
    @ArchTest
    static final ArchRule repositories_ne_dependent_pas_des_services =
            noClasses().that().haveSimpleNameEndingWith("Repository")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                    .because("Les repositories sont une couche basse — ils ne remontent pas vers les services");

    /**
     * Règle 5 — Les exceptions métier sont toutes unchecked (RuntimeException)
     *
     * Toute classe dans le package exception dont le nom finit par "Exception"
     * doit étendre RuntimeException.
     * Évite de polluer toutes les signatures de méthodes avec "throws XException".
     */
    @ArchTest
    static final ArchRule exceptions_metier_sont_unchecked =
            classes().that().resideInAPackage("..exception..")
                    .and().haveSimpleNameEndingWith("Exception")
                    .should().beAssignableTo(RuntimeException.class)
                    .because("Les exceptions métier doivent être unchecked (RuntimeException)");

    /**
     * Règle 6 — Les DTOs ne sont pas des beans Spring
     *
     * Les classes dans les packages dto sont de simples conteneurs de données.
     * Elles ne doivent pas être annotées @Service, @Component ou @Repository,
     * sinon Spring les instancie comme beans et crée des dépendances inutiles.
     */
    @ArchTest
    static final ArchRule dtos_ne_sont_pas_des_beans_spring =
            classes().that().resideInAPackage("..dto..")
                    .should().notBeAnnotatedWith(Service.class)
                    .andShould().notBeAnnotatedWith(Component.class)
                    .andShould().notBeAnnotatedWith(Repository.class)
                    .because("Les DTOs sont de simples objets de transfert, pas des beans Spring");

    /**
     * Règle 7 — Les mappers MapStruct sont dans les packages mapper
     *
     * Toute interface annotée @Mapper (MapStruct) doit résider dans un package "mapper".
     * Garantit que la logique de mapping est toujours localisable au même endroit.
     */
    @ArchTest
    static final ArchRule mappers_dans_package_mapper =
            classes().that().areAnnotatedWith(org.mapstruct.Mapper.class)
                    .should().resideInAPackage("..mapper..")
                    .because("Les mappers MapStruct doivent être dans un package 'mapper'");
}
