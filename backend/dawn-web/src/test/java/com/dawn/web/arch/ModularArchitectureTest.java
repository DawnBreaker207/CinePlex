package com.dawn.web.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ModularArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter().importPackages("com.dawn.booking", "com.dawn.catalog",
                "com.dawn.cinema", "com.dawn.identity", "com.dawn.notification", "com.dawn.payment",
                "com.dawn.report", "com.dawn.ai", "com.dawn.common");
    }

    private static final String INTERNAL_OTHER_BOOKING = "com.dawn.catalog.internal..";
    private static final String INTERNAL_OTHER_CINEMA = "com.dawn.cinema.internal..";
    private static final String INTERNAL_OTHER_IDENTITY = "com.dawn.identity.internal..";

    @Test
    void booking_mustNotImportInternalOfOtherModules() {
        ArchRule rule = noClasses().that().resideInAPackage("com.dawn.booking..")
                .should().accessClassesThat().resideInAnyPackage(
                        INTERNAL_OTHER_BOOKING, INTERNAL_OTHER_CINEMA, INTERNAL_OTHER_IDENTITY);
        rule.check(classes);
    }

    @Test
    void common_mustNotDependOnAnyModulePackage() {
        ArchRule rule = noClasses().that().resideInAPackage("com.dawn.common..")
                .should().accessClassesThat().resideInAnyPackage(
                        "com.dawn.booking..", "com.dawn.catalog..", "com.dawn.cinema..",
                        "com.dawn.identity..", "com.dawn.notification..", "com.dawn.payment..",
                        "com.dawn.report..", "com.dawn.ai..");
        rule.check(classes);
    }

    @Test
    void crossModuleDependencies_mustNotImportInternalImpl() {
        ArchRule rule = noClasses().that().resideOutsideOfPackage("com.dawn.catalog..")
                .should().dependOnClassesThat().resideInAPackage("com.dawn.catalog.internal.impl..");
        rule.check(classes);
    }
}