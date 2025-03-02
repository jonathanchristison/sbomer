/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.cli.test.command.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.commands.processor.RedHatProductProcessCommand;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.cli.test.PncWireMock;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.core.utils.SbomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.jboss.sbomer.core.utils.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.core.utils.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;
import static org.jboss.sbomer.core.utils.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;

@QuarkusTest
@QuarkusTestResource(PncWireMock.class)
public class RedHatProductProcessCommandTest {
    @Inject
    RedHatProductProcessCommand command;

    @Inject
    ObjectMapper objectMapper;

    private JsonNode generateBom() throws IOException {
        String bomJson = TestResources.asString("sboms/sbom-valid.json");
        return objectMapper.readTree(bomJson);
    }

    private Sbom generateSbom() throws IOException {
        return Sbom.builder().buildId("ARYT3LBXDVYAC").sbom(generateBom()).id(123456l).build();
    }

    @Test
    void shouldReturnCorrectImplementationType() {
        assertEquals(ProcessorImplementation.REDHAT_PRODUCT, command.getImplementationType());
    }

    @Test
    void shouldStopProcessingIfTheBuildIsNotFound() throws Exception {
        Sbom sbom = generateSbom();
        sbom.setBuildId("NOTEXISTING");

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom, SbomUtils.fromJsonNode(sbom.getSbom()));
        });

        assertEquals(
                "Could not obtain PNC Product Version information for the 'NOTEXISTING' PNC build, interrupting processing",
                ex.getMessage());

    }

    @Test
    void generatedSbomShouldNotHaveProductMetadata() throws IOException {
        Sbom sbom = generateSbom();

        Bom bom = SbomUtils.fromJsonNode(sbom.getSbom());

        assertFalse(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_NAME));
        assertFalse(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VERSION));
        assertFalse(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VARIANT));
    }

    @Test
    // TODO: Add tests for logs
    void shouldStopWhenBuildConfigIsNotFound() throws Exception {
        Sbom sbom = generateSbom();
        sbom.setBuildId("MISSINGBUILDCONFIG");

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom, SbomUtils.fromJsonNode(sbom.getSbom()));
        });

        assertEquals(
                "Could not obtain PNC Product Version information for the 'MISSINGBUILDCONFIG' PNC build, interrupting processing",
                ex.getMessage());
    }

    @Test
    void shouldFailOnMissingMapping() throws Exception {
        Sbom sbom = generateSbom();

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom, SbomUtils.fromJsonNode(sbom.getSbom()));
        });

        assertEquals("Could not find mapping for the PNC Product Version '1.0' (id: 179)", ex.getMessage());
    }

    @Test
    @DisplayName("Should run the processor successfully")
    void shouldProcess() throws Exception {
        Sbom sbom = generateSbom();
        sbom.setBuildId("QUARKUS");
        Bom bom = command.doProcess(sbom, SbomUtils.fromJsonNode(sbom.getSbom()));

        assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_NAME));
        assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VERSION));
        assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VARIANT));

        assertEquals(
                "RHBQ",
                SbomUtils
                        .findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_NAME, bom.getMetadata().getComponent())
                        .get()
                        .getValue());
        assertEquals(
                "RHEL-8-RHBQ-2.13",
                SbomUtils
                        .findPropertyWithNameInComponent(
                                PROPERTY_ERRATA_PRODUCT_VERSION,
                                bom.getMetadata().getComponent())
                        .get()
                        .getValue());
        assertEquals(
                "8Base-RHBQ-2.13",
                SbomUtils
                        .findPropertyWithNameInComponent(
                                PROPERTY_ERRATA_PRODUCT_VARIANT,
                                bom.getMetadata().getComponent())
                        .get()
                        .getValue());
    }

    @Test
    @DisplayName("Should interrupt processing on missing product version")
    void shouldMissingProductVersion() throws Exception {
        Sbom sbom = generateSbom();
        sbom.setBuildId("MISSINGPRODUCTVERSION");

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom, SbomUtils.fromJsonNode(sbom.getSbom()));
        });

        assertEquals(
                "Could not obtain PNC Product Version information for the 'MISSINGPRODUCTVERSION' PNC build, interrupting processing",
                ex.getMessage());
    }

}
