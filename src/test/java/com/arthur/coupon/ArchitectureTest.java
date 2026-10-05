package com.arthur.coupon;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.arthur.coupon", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnOtherLayersNorFrameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..", "..infrastructure..",
                            "org.springframework..", "jakarta..", "javax..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructureNorFrameworks =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infrastructure..",
                            "org.springframework..", "jakarta..", "javax..");

    @ArchTest
    static final ArchRule useCasesExposeASinglePublicMethod =
            classes().that().haveSimpleNameEndingWith("UseCase")
                    .should(haveExactlyOnePublicMethod());

    private static ArchCondition<JavaClass> haveExactlyOnePublicMethod() {
        return new ArchCondition<>("have exactly one public method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long publicMethods = javaClass.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .count();
                events.add(new SimpleConditionEvent(javaClass, publicMethods == 1,
                        javaClass.getName() + " has " + publicMethods + " public methods"));
            }
        };
    }
}
