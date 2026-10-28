package com.pro24_group4.pro24_group4;

import com.jayway.jsonpath.JsonPath;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.entity.User;
import com.pro24_group4.pro24_group4.repository.RoleRepository;
import com.pro24_group4.pro24_group4.repository.UserRepository;
import com.pro24_group4.pro24_group4.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${app.seed.admin-password:AdminTest123!}")
    private String adminPassword;

    private String generateExpiredToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis() - 200_000))
                .expiration(new Date(System.currentTimeMillis() - 100_000))
                .signWith(key)
                .compact();
    }

    @Test
    void testRegisterSuccess() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String requestJson = """
                {
                    "username": "%s",
                    "password": "Password123!",
                    "nickName": "TestUser"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void testRegisterDuplicateUsernameFails() throws Exception {
        String username = "dup_" + UUID.randomUUID().toString().substring(0, 8);
        String requestJson = """
                {
                    "username": "%s",
                    "password": "Password123!",
                    "nickName": "DuplicateUser"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Attempting to register again with same username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testLoginSuccess() throws Exception {
        String username = "login_" + UUID.randomUUID().toString().substring(0, 8);
        String registerJson = """
                {
                    "username": "%s",
                    "password": "Password123!",
                    "nickName": "LoginUser"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        String loginJson = """
                {
                    "username": "%s",
                    "password": "Password123!"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void testLoginWrongPasswordReturns401() throws Exception {
        String loginJson = """
                {
                    "username": "admin",
                    "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testLoginLockedAccountReturns401() throws Exception {
        String username = "locked_" + UUID.randomUUID().toString().substring(0, 8);
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .nickName("Locked User")
                .enabled(true)
                .accountNonLocked(false)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        String loginJson = """
                {
                    "username": "%s",
                    "password": "Password123!"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testLoginDisabledAccountReturns401() throws Exception {
        String username = "disabled_" + UUID.randomUUID().toString().substring(0, 8);
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .nickName("Disabled User")
                .enabled(false)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        String loginJson = """
                {
                    "username": "%s",
                    "password": "Password123!"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testMeWithTokenReturns200() throws Exception {
        String token = jwtService.generateToken("admin");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void testMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testExpiredTokenReturns401() throws Exception {
        String expiredToken = generateExpiredToken("admin");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer this-is-not-a-valid-jwt-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testRefreshSuccess() throws Exception {
        String token = jwtService.generateToken("admin");
        String refreshJson = """
                {
                    "token": "%s"
                }
                """.formatted(token);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        String newToken = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        assertThat(newToken).isNotBlank();
        assertThat(jwtService.extractUsername(newToken)).isEqualTo("admin");
    }
}
