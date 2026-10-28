package com.pro24_group4.pro24_group4;

import com.jayway.jsonpath.JsonPath;
import com.pro24_group4.pro24_group4.authorization.Authorities;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testNoTokenReturns401OnProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testDaraPermissionsCheck() throws Exception {
        String daraToken = jwtService.generateToken("dara");

        // dara has USER_READ: GET /api/users is 200
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + daraToken))
                .andExpect(status().isOk());

        // dara lacks USER_WRITE: POST /api/users is 403
        String newUserJson = """
                {
                    "username": "user_%s",
                    "password": "Password123!",
                    "nickName": "Test"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + daraToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // dara lacks USER_WRITE: PUT /api/users/1 is 403
        mockMvc.perform(put("/api/users/1")
                        .header("Authorization", "Bearer " + daraToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickName\":\"Updated\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // dara lacks USER_DELETE: DELETE /api/users/2 is 403 (Poster Acceptance Check)
        mockMvc.perform(delete("/api/users/2")
                        .header("Authorization", "Bearer " + daraToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testAdminCanDoEverything() throws Exception {
        String adminToken = jwtService.generateToken("admin");

        // Admin can GET users
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin can GET roles
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin can GET permissions
        mockMvc.perform(get("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin can access /api/test
        mockMvc.perform(get("/api/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDynamicRoleAssignmentAndRevokeWithSameToken() throws Exception {
        String adminToken = jwtService.generateToken("admin");

        // 1. Register a fresh user
        String freshUsername = "fresh_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "Password123!",
                            "nickName": "Fresh"
                        }
                        """.formatted(freshUsername)))
                .andExpect(status().isOk())
                .andReturn();

        String freshToken = JsonPath.read(regResult.getResponse().getContentAsString(), "$.token");
        User freshUser = userRepository.findByUsername(freshUsername).orElseThrow();
        Long freshUserId = freshUser.getId();

        Role adminRole = roleRepository.findByName(Authorities.ROLE_ADMIN).orElseThrow();
        Long adminRoleId = adminRole.getId();

        // Fresh user currently has ROLE_USER only, so deleting a user gives 403
        mockMvc.perform(delete("/api/users/999999")
                        .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isForbidden());

        // 2. Admin assigns ROLE_ADMIN to fresh user -> 200 (Poster Acceptance Check)
        mockMvc.perform(post("/api/users/" + freshUserId + "/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 3. That user's SAME token gains access immediately!
        // Calling DELETE /api/users/999999 now reaches the controller (returns 404 because user not found, not 403!)
        mockMvc.perform(delete("/api/users/999999")
                        .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isNotFound());

        // 4. Admin revokes ROLE_ADMIN from fresh user -> 200
        mockMvc.perform(delete("/api/users/" + freshUserId + "/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 5. With the SAME token, user loses access immediately -> 403!
        mockMvc.perform(delete("/api/users/999999")
                        .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSoftDeletedUserTokenReturns401() throws Exception {
        String username = "deluser_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "Password123!",
                            "nickName": "ToDelete"
                        }
                        """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(regResult.getResponse().getContentAsString(), "$.token");

        // Verify token works
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Soft delete user in database
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Subsequent call with same token returns 401
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
