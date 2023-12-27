package io.phasetwo.service.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.phasetwo.client.openapi.model.OrganizationRepresentation;
import io.phasetwo.service.AbstractOrganizationTest;
import io.phasetwo.service.resource.OrganizationAdminAuth;
import io.restassured.response.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.jbosslog.JBossLog;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.phasetwo.service.Helpers.createUserWithCredentials;
import static io.phasetwo.service.Helpers.deleteUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@JBossLog
@Testcontainers
public class OrganizationProfileTest extends AbstractOrganizationTest {

  private static final String FIELD_NAME_VALUE = "name";
  private static final String ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY = "admin_org_ro_test";
  private static final String ORG_RO_ATTRIBUTE_TEST_KEY = "org_ro_test";
  private static final String ATTRIBUTE_TEST_KEY = "test";
  private static final String TEST_VALUE = "TEST";
  private static final String TEST_DOMAIN_VALUE = "test.com";
  private static final String SAMPLE_NAME = "sample";
  private static final String ERROR_MESSAGE_KEY = "errorMessage";
  private static final String FIELD_KEY = "field";
  private static final String READ_ONLY_REJECTED_MESSAGE = "update-read-only-attributes-rejected-message";
  private static final String READ_ONLY_NAME_REJECTED_MESSAGE = "update-read-only-name-rejected-message";

  @Test
  void testKeycloakAdminOrganizationAttributeUpdate() throws IOException {
    // Test in the scope of Platform Administrator (and not org manager)
    Map<String, List<String>> originalAttributes = new HashMap<>();
    originalAttributes.put(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("1"));
    originalAttributes.put(ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("1"));
    originalAttributes.put(ATTRIBUTE_TEST_KEY, Collections.singletonList("1"));

    OrganizationRepresentation rep = new OrganizationRepresentation().name(SAMPLE_NAME)
        .displayName(SAMPLE_NAME)
        .attributes(originalAttributes).domains(List.of("sample.com"));
    rep = createOrganization(rep);
    String orgId = rep.getId();

    // verify organization
    Response response = getRequest(keycloak, orgId);
    assertThat(response.getStatusCode(), is(Status.OK.getStatusCode()));
    // get OrganizationRepresentation
    rep = new ObjectMapper().readValue(response.getBody().asString(), new TypeReference<>() {
    });
    Map<String, List<String>> attributes = rep.getAttributes();

    // try updating admin read only attribute
    attributes.put(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("2"));
    rep.setAttributes(attributes);
    response = putRequest(keycloak, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.BAD_REQUEST.getStatusCode()));
    validateErrorMessage(new ObjectMapper().readTree(response.body().asString()),
        ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY,
        READ_ONLY_REJECTED_MESSAGE);

    // try removing admin read only attribute (should return 204 but not remove it)
    rep = new ObjectMapper().readValue(getRequest(keycloak, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    attributes = rep.getAttributes();
    attributes.remove(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY);
    rep.setAttributes(attributes);
    response = putRequest(keycloak, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(keycloak, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    // assert that the attribute wasn't actually removed
    assertThat(rep.getAttributes().containsKey(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY), is(true));
    //

    // try changing organization name, display name and domains
    rep.setName(TEST_VALUE);
    rep.setDisplayName(TEST_VALUE);
    rep.setDomains(List.of(TEST_DOMAIN_VALUE));
    response = putRequest(keycloak, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    response = getRequest(keycloak, orgId);
    rep = new ObjectMapper().readValue(response.getBody().asString(), new TypeReference<>() {
    });
    assertThat(rep.getName(), is(TEST_VALUE));
    assertThat(rep.getDisplayName(), is(TEST_VALUE));
    assertThat(rep.getDomains().size(), is(1));
    assertThat(rep.getDomains().contains(TEST_DOMAIN_VALUE), is(true));
    //

    // try changing standard attribute & no-admin read only attribute
    attributes = rep.getAttributes();
    attributes.put(ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("2"));
    attributes.put(ATTRIBUTE_TEST_KEY, Collections.singletonList("2"));
    rep.setAttributes(attributes);
    response = putRequest(keycloak, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(keycloak, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().get(ORG_RO_ATTRIBUTE_TEST_KEY),
        is(Collections.singletonList("2")));
    assertThat(rep.getAttributes().get(ATTRIBUTE_TEST_KEY), is(Collections.singletonList("2")));
    //

    // try removing standard attribute & removing no-admin read only attribute
    attributes = rep.getAttributes();
    attributes.remove(ORG_RO_ATTRIBUTE_TEST_KEY);
    attributes.remove(ATTRIBUTE_TEST_KEY);
    rep.setAttributes(attributes);
    response = putRequest(keycloak, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(keycloak, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().containsKey(ORG_RO_ATTRIBUTE_TEST_KEY), is(false));
    assertThat(rep.getAttributes().containsKey(ATTRIBUTE_TEST_KEY), is(false));

    // try adding standard attribute & no-admin read only attribute
    attributes = rep.getAttributes();
    attributes.put(ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList(TEST_VALUE));
    attributes.put(ATTRIBUTE_TEST_KEY, Collections.singletonList(TEST_VALUE));
    rep.setAttributes(attributes);
    response = putRequest(keycloak, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(keycloak, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().get(ORG_RO_ATTRIBUTE_TEST_KEY),
        is(Collections.singletonList(TEST_VALUE)));
    assertThat(rep.getAttributes().get(ATTRIBUTE_TEST_KEY),
        is(Collections.singletonList(TEST_VALUE)));

    // delete org
    deleteOrganization(orgId);
  }

  @Test
  void testOrganizationUserOrganizationAttributeUpdate() throws IOException {
    // Test in the scope of an organization manager (admin)
    Map<String, List<String>> originalAttributes = new HashMap<>();
    originalAttributes.put(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("1"));
    originalAttributes.put(ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("1"));
    originalAttributes.put(ATTRIBUTE_TEST_KEY, Collections.singletonList("1"));

    OrganizationRepresentation rep = new OrganizationRepresentation().name(SAMPLE_NAME)
        .displayName(SAMPLE_NAME)
        .attributes(originalAttributes).domains(List.of("sample.com"));
    rep = createOrganization(rep);
    String orgId = rep.getId();

    // add standard user with manage privileges
    UserRepresentation manager = createUserWithCredentials(keycloak, REALM, "manager", "password");
    Response response = putRequest("pass", orgId, "members", manager.getId());
    assertThat(response.getStatusCode(), is(Status.CREATED.getStatusCode()));
    for (String role : OrganizationAdminAuth.DEFAULT_ORG_ROLES) {
      grantUserRole(orgId, role, manager.getId());
    }
    Keycloak kc = getKeycloak(REALM, "admin-cli", "manager", "password");

    // get organization
    response = getRequest(kc, orgId);
    assertThat(response.getStatusCode(), is(Status.OK.getStatusCode()));
    rep = new ObjectMapper().readValue(response.getBody().asString(), new TypeReference<>() {
    });
    Map<String, List<String>> attributes = rep.getAttributes();

    // validate no access to /manage
    response = putRequest(kc, rep, "/%s/manage".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.UNAUTHORIZED.getStatusCode()));

    // try updating admin read only attribute
    attributes.put(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("2"));
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.BAD_REQUEST.getStatusCode()));
    validateErrorMessage(new ObjectMapper().readTree(response.body().asString()),
        ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY,
        READ_ONLY_REJECTED_MESSAGE);
    //

    // try removing admin read only attribute (should return 204 but not remove it)
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    attributes = rep.getAttributes();
    attributes.remove(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY);
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    // assert that the attribute wasn't actually removed
    assertThat(rep.getAttributes().containsKey(ADMIN_ORG_RO_ATTRIBUTE_TEST_KEY), is(true));
    //

    // try changing organization name
    rep.setName(TEST_VALUE);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.BAD_REQUEST.getStatusCode()));
    validateErrorMessage(new ObjectMapper().readTree(response.body().asString()), FIELD_NAME_VALUE,
        READ_ONLY_NAME_REJECTED_MESSAGE);
    //

    // try changing display name and domains
    rep.setName(SAMPLE_NAME);
    rep.setDisplayName(TEST_VALUE);
    rep.setDomains(List.of(TEST_DOMAIN_VALUE));
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getName(), is(SAMPLE_NAME));
    assertThat(rep.getDisplayName(), is(TEST_VALUE));
    assertThat(rep.getDomains().size(), is(1));
    assertThat(rep.getDomains().contains(TEST_DOMAIN_VALUE), is(true));
    //

    // try changing read only attribute
    attributes = rep.getAttributes();
    attributes.put(ORG_RO_ATTRIBUTE_TEST_KEY, Collections.singletonList("2"));
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.BAD_REQUEST.getStatusCode()));
    validateErrorMessage(new ObjectMapper().readTree(response.body().asString()),
        ORG_RO_ATTRIBUTE_TEST_KEY,
        READ_ONLY_REJECTED_MESSAGE);
    //

    // try removing read only attribute (should return 204 but not remove it)
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    attributes = rep.getAttributes();
    attributes.remove(ORG_RO_ATTRIBUTE_TEST_KEY);
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().containsKey(ORG_RO_ATTRIBUTE_TEST_KEY), is(true));
    assertThat(rep.getAttributes().get(ORG_RO_ATTRIBUTE_TEST_KEY),
        is(Collections.singletonList("1")));
    //

    // try changing standard attribute
    attributes = rep.getAttributes();
    attributes.put(ATTRIBUTE_TEST_KEY, Collections.singletonList("2"));
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().get(ATTRIBUTE_TEST_KEY), is(Collections.singletonList("2")));
    //

    // try removing standard attribute
    attributes.remove(ATTRIBUTE_TEST_KEY);
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().containsKey(ATTRIBUTE_TEST_KEY), is(false));
    //

    // try adding standard attribute
    attributes.put(ATTRIBUTE_TEST_KEY, Collections.singletonList(TEST_VALUE));
    rep.setAttributes(attributes);
    response = putRequest(kc, rep, "/%s".formatted(orgId));
    assertThat(response.getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
    rep = new ObjectMapper().readValue(getRequest(kc, orgId).getBody().asString(),
        new TypeReference<>() {
        });
    assertThat(rep.getAttributes().containsKey(ATTRIBUTE_TEST_KEY), is(true));
    assertThat(rep.getAttributes().get(ATTRIBUTE_TEST_KEY),
        is(Collections.singletonList(TEST_VALUE)));
    //

    // delete user
    deleteUser(keycloak, REALM, manager.getId());
    // delete org
    deleteOrganization(orgId);
  }

  private void validateErrorMessage(JsonNode jsonNode, String fieldValue,
      String errorMessageValue) {
    assertThat(jsonNode.hasNonNull(FIELD_KEY), is(true));
    assertThat(jsonNode.get(FIELD_KEY).asText(), is(fieldValue));
    assertThat(jsonNode.hasNonNull(ERROR_MESSAGE_KEY), is(true));
    assertThat(jsonNode.get(ERROR_MESSAGE_KEY).asText(), is(errorMessageValue));
  }
}
