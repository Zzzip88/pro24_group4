package com.pro24_group4.pro24_group4;

import com.jayway.jsonpath.JsonPath;
import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.repository.PermissionRepository;
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
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void testPermissionCrudAndSoftDelete() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        String permName = "PERM_TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        // 1. Create Permission (POST 201)
        String createJson = """
                {
                    "name": "%s",
                    "description": "Custom permission"
                }
                """.formatted(permName);

        MvcResult createResult = mockMvc.perform(post("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(permName))
                .andReturn();

        Number idNum = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        Long permId = idNum.longValue();

        // 2. Get Permission By ID (GET 200)
        mockMvc.perform(get("/api/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(permId))
                .andExpect(jsonPath("$.name").value(permName));

        // 3. Get All Permissions (GET 200)
        mockMvc.perform(get("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. Update Permission (PUT 200)
        String updatedPermName = permName + "_MOD";
        String updateJson = """
                {
                    "name": "%s",
                    "description": "Updated description"
                }
                """.formatted(updatedPermName);

        mockMvc.perform(put("/api/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedPermName));

        // 5. Delete Permission (DELETE 204)
        mockMvc.perform(delete("/api/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 6. Verify row stays in table with deleted_at set
        Optional<Permission> dbPermOpt = permissionRepository.findById(permId);
        assertThat(dbPermOpt).isPresent();
        assertThat(dbPermOpt.get().getDeletedAt()).isNotNull();

        // 7. Subsequent GET returns 404
        mockMvc.perform(get("/api/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testPermissionValidationFails400() throws Exception {
        String adminToken = jwtService.generateToken("admin");
        String invalidJson = """
                {
                    "name": ""
                }
                """;

        mockMvc.perform(post("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testDuplicatePermissionFails409() throws Exception {
        String adminToken = jwtService.generateToken("admin");

        String dupJson = """
                {
                    "name": "USER_READ",
                    "description": "Duplicate"
                }
                """;

        mockMvc.perform(post("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
