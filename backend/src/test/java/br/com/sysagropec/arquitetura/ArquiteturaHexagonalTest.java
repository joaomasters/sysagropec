package br.com.sysagropec.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras da arquitetura hexagonal verificadas no bytecode. Uma violação quebra o build.
 */
class ArquiteturaHexagonalTest {

    static final String BASE = "br.com.sysagropec";
    static final List<String> MODULOS = List.of("identidade", "propriedades", "rebanho", "pesagem", "tarefas",
            "consultas");

    static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void dominioEhJavaPuro() {
        classes().that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", BASE + ".shared.domain..", "..domain..")
                .because("o domínio não depende de frameworks, persistência nem da camada de aplicação")
                .check(classes);
    }

    @Test
    void aplicacaoNaoDependeDeFrameworksNemDeAdaptadores() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.servlet..",
                        "org.hibernate..", "..adapter..", "..configuration..")
                .check(classes);
    }

    @Test
    void portasNaoExpoemTiposDoSpringOuJpa() {
        noClasses().that().resideInAPackage("..application.port..")
                .should().beAssignableTo(org.springframework.data.repository.Repository.class)
                .orShould().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                .check(classes);
    }

    @Test
    void entidadesJpaFicamNoAdaptadorDePersistencia() {
        classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..adapter.out.persistence..")
                .andShould().notBePublic()
                .check(classes);
    }

    @Test
    void adaptadoresDeEntradaNaoAcessamAdaptadoresDeSaida() {
        noClasses().that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                .check(classes);
    }

    @Test
    void controllersUsamApenasPortasDeEntrada() {
        noClasses().that().resideInAPackage("..adapter.in.web..")
                .should().dependOnClassesThat().resideInAnyPackage("..application.usecase..", "..application.port.out..",
                        "org.springframework.data..", "jakarta.persistence..")
                .check(classes);
    }

    @Test
    void sharedNaoConheceOsModulos() {
        noClasses().that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        MODULOS.stream().map(m -> BASE + "." + m + "..").toArray(String[]::new))
                .check(classes);
    }

    /** Entre módulos, apenas contratos públicos: as portas de entrada ({@code application.port.in}). */
    @Test
    void modulosSoSeComunicamPorPortasDeEntrada() {
        for (String modulo : MODULOS) {
            String pacote = BASE + "." + modulo;
            noClasses().that().resideOutsideOfPackage(pacote + "..")
                    .should().dependOnClassesThat(new DescribedPredicate<>("internas do módulo " + modulo) {
                        @Override
                        public boolean test(JavaClass c) {
                            return c.getPackageName().startsWith(pacote + ".")
                                    && !c.getPackageName().startsWith(pacote + ".application.port.in");
                        }
                    })
                    .check(classes);
        }
    }

    /** Dentro dos módulos, a lógica de negócio não chama outro módulo diretamente: usa portas de saída. */
    @Test
    void nucleoNaoDependeDeOutrosModulos() {
        for (String modulo : MODULOS) {
            String pacote = BASE + "." + modulo;
            noClasses().that().resideInAnyPackage(pacote + ".domain..", pacote + ".application..")
                    .should().dependOnClassesThat(new DescribedPredicate<>("de outros módulos") {
                        @Override
                        public boolean test(JavaClass c) {
                            String p = c.getPackageName();
                            return p.startsWith(BASE + ".") && !p.startsWith(pacote) && !p.startsWith(BASE + ".shared");
                        }
                    })
                    .check(classes);
        }
    }

    @Test
    void transacoesNaoSaoDeclaradasNoNucleo() {
        noClasses().that().resideInAnyPackage("..domain..", "..application..")
                .should(new ArchCondition<>("usar @Transactional") {
                    final Set<String> anotacoes = Set.of("org.springframework.transaction.annotation.Transactional",
                            "jakarta.transaction.Transactional");

                    @Override
                    public void check(JavaClass c, ConditionEvents events) {
                        boolean usa = c.getAnnotations().stream().anyMatch(a -> anotacoes.contains(a.getRawType().getName()))
                                || c.getMethods().stream().anyMatch(m -> m.getAnnotations().stream()
                                .anyMatch(a -> anotacoes.contains(a.getRawType().getName())));
                        if (usa) {
                            events.add(SimpleConditionEvent.violated(c, c.getName() + " usa @Transactional"));
                        }
                    }
                })
                .check(classes);
    }
}
