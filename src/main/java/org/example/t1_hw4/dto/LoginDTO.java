package org.example.t1_hw4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginDTO {
    @NotBlank(message = "login cannot be empty")
    @Size(min = 4, max = 100)
    public String login;

    @NotBlank(message = "password cannot be empty")
    @Size(min = 8, max = 100)
    public String password;
}
