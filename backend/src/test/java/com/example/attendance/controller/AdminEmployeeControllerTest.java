package com.example.attendance.controller;

import com.example.attendance.dto.CreateEmployeeRequest;
import com.example.attendance.dto.LoginRequest;
import com.example.attendance.dto.UpdateEmployeeRequest;
import com.example.attendance.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /admin/employees: ADMIN → 200で社員一覧が返る")
    void findAll_admin_returns200() throws Exception {
        MockHttpSession session = adminSession();

        mockMvc.perform(get("/admin/employees").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employeeCode").value("EMP001"));
    }

    @Test
    @DisplayName("GET /admin/employees: 未認証でもアクセス可能")
    void findAll_unauthenticated_returns200() throws Exception {
        mockMvc.perform(get("/admin/employees"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /admin/employees: 正常登録 → 201")
    void create_validRequest_returns201() throws Exception {
        MockHttpSession session = adminSession();
        var request = new CreateEmployeeRequest("新社員", "new-emp@example.com", "password123", Role.EMPLOYEE);

        mockMvc.perform(post("/admin/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("新社員"))
                .andExpect(jsonPath("$.email").value("new-emp@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.employeeCode").exists());
    }

    @Test
    @DisplayName("POST /admin/employees: メール重複 → 409")
    void create_duplicateEmail_returns409() throws Exception {
        MockHttpSession session = adminSession();
        var request = new CreateEmployeeRequest("重複社員", "admin@example.com", "password123", Role.EMPLOYEE);

        mockMvc.perform(post("/admin/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("このメールアドレスは既に使用されています"));
    }

    @Test
    @DisplayName("POST /admin/employees: バリデーションエラー → 400")
    void create_invalidRequest_returns400() throws Exception {
        MockHttpSession session = adminSession();
        var request = new CreateEmployeeRequest("", "invalid-email", "short", null);

        mockMvc.perform(post("/admin/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("GET /admin/employees/{id}: 存在するID → 200")
    void findById_existingId_returns200() throws Exception {
        MockHttpSession session = adminSession();

        mockMvc.perform(get("/admin/employees/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode").value("EMP001"));
    }

    @Test
    @DisplayName("GET /admin/employees/{id}: 存在しないID → 404")
    void findById_nonExistingId_returns404() throws Exception {
        MockHttpSession session = adminSession();

        mockMvc.perform(get("/admin/employees/999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /admin/employees/{id}: 正常更新 → 200")
    void update_validRequest_returns200() throws Exception {
        MockHttpSession session = adminSession();

        var createReq = new CreateEmployeeRequest("更新対象", "update-target@example.com", "password123", Role.EMPLOYEE);
        MvcResult createResult = mockMvc.perform(post("/admin/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        Long createdId = objectMapper.readTree(body).get("id").asLong();
        Long version = objectMapper.readTree(body).get("version").asLong();

        var updateReq = new UpdateEmployeeRequest("更新済み社員", "update-target@example.com", Role.ADMIN, version);

        mockMvc.perform(put("/admin/employees/" + createdId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新済み社員"));
    }

    @Test
    @DisplayName("DELETE /admin/employees/{id}: 物理削除 → 204")
    void delete_existingId_returns204() throws Exception {
        MockHttpSession session = adminSession();

        // まず社員を作る
        var createReq = new CreateEmployeeRequest("削除対象", "delete-target@example.com", "password123", Role.EMPLOYEE);
        MvcResult createResult = mockMvc.perform(post("/admin/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long createdId = objectMapper.readTree(responseBody).get("id").asLong();

        // 物理削除
        mockMvc.perform(delete("/admin/employees/" + createdId).session(session))
                .andExpect(status().isNoContent());

        // 一覧に含まれないことを確認
        mockMvc.perform(get("/admin/employees").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'delete-target@example.com')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /admin/employees: EMPLOYEEでもアクセス可能")
    void findAll_employee_returns200() throws Exception {
        MockHttpSession session = employeeSession();

        mockMvc.perform(get("/admin/employees").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /admin/employees/{id}: version不一致 → 409")
    void update_versionMismatch_returns409() throws Exception {
        MockHttpSession session = adminSession();

        var createReq = new CreateEmployeeRequest("楽観ロック対象", "optimistic@example.com", "password123", Role.EMPLOYEE);
        MvcResult createResult = mockMvc.perform(post("/admin/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        Long createdId = objectMapper.readTree(body).get("id").asLong();

        var updateReq = new UpdateEmployeeRequest("更新", "optimistic@example.com", Role.EMPLOYEE, 999L);

        mockMvc.perform(put("/admin/employees/" + createdId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());
    }

    private MockHttpSession adminSession() throws Exception {
        var request = new LoginRequest("admin@example.com", "admin1234");
        var result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    private MockHttpSession employeeSession() throws Exception {
        MockHttpSession adminSess = adminSession();
        var createReq = new CreateEmployeeRequest("一般社員", "emp-test@example.com", "password123", Role.EMPLOYEE);
        mockMvc.perform(post("/admin/employees")
                        .session(adminSess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        var loginReq = new LoginRequest("emp-test@example.com", "password123");
        var result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }
}
