package com.pro24_group4.pro24_group4;

import com.jayway.jsonpath.JsonPath;
import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.repository.PermissionRepository;
import com.pro24_group4.pro24_group4.repository.RoleRepository;
import com.pro24_group4.pro24_group4.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void testRoleCrudAndPermissions() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        String roleName = "ROLE_CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        // 1. Create Role (POST 201)
        String createJson = """
                {
                    "name": "%s",
                    "description": "Custom role for testing"
                }
                """.formatted(roleName);

        MvcResult createResult = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(roleName))
                .andReturn();

        Number idNum = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        Long roleId = idNum.longValue();

        // 2. Get Role By ID (GET 200)
        mockMvc.perform(get("/api/roles/" + roleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleId))
                .andExpect(jsonPath("$.name").value(roleName));

        // 3. Get All Roles (GET 200)
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. Update Role (PUT 200)
        String updatedRoleName = roleName + "_MOD";
        String updateJson = """
                {
                    "name": "%s",
                    "description": "Updated custom role"
                }
                """.formatted(updatedRoleName);

        mockMvc.perform(put("/api/roles/" + roleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedRoleName));

        // 5. Assign and Revoke Permission
        Permission perm = permissionRepository.findByName("USER_READ").orElseThrow();
        Long permId = perm.getId();

        mockMvc.perform(post("/api/roles/" + roleId + "/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/roles/" + roleId + "/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 6. Delete Role (DELETE 204)
        mockMvc.perform(delete("/api/roles/" + roleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 7. Verify soft deletion: Row stays in table, deleted_at is set
        Optional<Role> dbRoleOpt = roleRepository.findById(roleId);
        assertThat(dbRoleOpt).isPresent();
        assertThat(dbRoleOpt.get().getDeletedAt()).isNotNull();

        // 8. Subsequent GET returns 404
        mockMvc.perform(get("/api/roles/" + roleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testRoleValidationFails400() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        // Name is blank
        String invalidJson = """
                {
                    "name": "",
                    "description": "Blank name"
                }
                """;

        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testDuplicateRoleFails409() throws Exception {
        String adminToken = jwtService.generateToken("admin");

        String dupJson = """
                {
                    "name": "ROLE_ADMIN",
                    "description": "Duplicate admin role"
                }
                """;

        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testCannotDeleteReservedRoleAdmin409() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();

        mockMvc.perform(delete("/api/roles/" + adminRole.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
