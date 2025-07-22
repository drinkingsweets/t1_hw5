package org.example.t1_hw4.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.t1_hw4.model.UserRole;

@NoArgsConstructor
@Getter
@Setter
public class UserDTO {
    private String login;
    private UserRole role;
    private String email;
}
