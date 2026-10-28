package com.pro24_group4.pro24_group4;

import com.jayway.jsonpath.JsonPath;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.entity.User;
import com.pro24_group4.pro24_group4.repository.RoleRepository;
import com.pro24_group4.pro24_group4.repository.UserRepository;
import com.pro24_group4.pro24_group4.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testUserCrudAndSoftDeleteAndNoPasswordInResponse() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        String username = "crud_" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Create User (POST 201)
        String createJson = """
                {
                    "username": "%s",
                    "password": "Password123!",
                    "nickName": "InitialNick"
                }
                """.formatted(username);

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.nickName").value("InitialNick"))
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        assertThat(createResponseBody).doesNotContain("password");
        assertThat(createResponseBody).doesNotContain("passwordHash");

        Number idNum = JsonPath.read(createResponseBody, "$.id");
        Long userId = idNum.longValue();

        // 2. Get User By ID (GET 200)
        MvcResult getResult = mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();

        String getResponseBody = getResult.getResponse().getContentAsString();
        assertThat(getResponseBody).doesNotContain("password");
        assertThat(getResponseBody).doesNotContain("passwordHash");

        // 3. Get All Users (GET 200)
        MvcResult listResult = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();
        assertThat(listResult.getResponse().getContentAsString()).doesNotContain("passwordHash");

        // 4. Update User (PUT 200)
        String updateJson = """
                {
                    "nickName": "UpdatedNick"
                }
                """;

        MvcResult updateResult = mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickName").value("UpdatedNick"))
                .andReturn();
        assertThat(updateResult.getResponse().getContentAsString()).doesNotContain("password");

        // 5. Assign and Revoke Role
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Long adminRoleId = adminRole.getId();

        mockMvc.perform(post("/api/users/" + userId + "/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Effective permissions endpoint
        mockMvc.perform(get("/api/users/" + userId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(delete("/api/users/" + userId + "/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 6. Delete User: 204 response (Poster Acceptance Check)
        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 7. Verification: Row stays in table, deleted_at is set (Poster Acceptance Check)
        Optional<User> dbUserOpt = userRepository.findById(userId);
        assertThat(dbUserOpt).isPresent();
        assertThat(dbUserOpt.get().getDeletedAt()).isNotNull();

        // 8. Subsequent GET returns 404 (soft-deleted resource)
        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testCreateUserValidationFails400() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        // Missing username and password
        String invalidJson = """
                {
                    "nickName": "NoCreds"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void testCreateDuplicateUserFails409() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        String username = "dup_" + UUID.randomUUID().toString().substring(0, 8);

        String json = """
                {
                    "username": "%s",
                    "password": "Password123!",
                    "nickName": "Original"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Second create with duplicate username
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testGetUserNotFound404() throws Exception {
        String adminToken = jwtService.generateToken("admin");

        mockMvc.perform(get("/api/users/888888")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
