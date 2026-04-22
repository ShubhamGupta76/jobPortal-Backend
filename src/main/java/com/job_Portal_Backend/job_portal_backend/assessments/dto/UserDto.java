package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String headline;

    public static UserDto fromUser(com.job_Portal_Backend.job_portal_backend.entity.User user) {
        if (user == null)
            return null;
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getHeadline());
    }
}
