package com.example.ums.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for POST (create) and PUT (update) on /api/admins, /api/teachers and /api/students.
 * <ul>
 *   <li>password: required on create, optional on update (omit to keep the old one)</li>
 *   <li>designation: used by teachers only</li>
 *   <li>studentIdNo, semester: used by students only</li>
 * </ul>
 */
public record UserRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(max = 100) String department,
        @Size(max = 100) String designation,
        @Size(max = 30) String studentIdNo,
        @Min(1) @Max(12) Integer semester
) {
}
