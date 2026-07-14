package com.example.attendance.dto;

import com.example.attendance.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank(message = "氏名は必須です")
        @Size(max = 100, message = "氏名は100文字以内で入力してください")
        String name,

        @NotBlank(message = "メールアドレスは必須です")
        @Email(message = "メールアドレスの形式が不正です")
        String email,

        @NotBlank(message = "パスワードは必須です")
        @Size(min = 8, message = "パスワードは8文字以上で入力してください")
        String password,

        @NotNull(message = "ロールは必須です")
        Role role
) {}
