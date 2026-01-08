package br.jus.tjpa.jsecurity.model.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginInput {

    @NotBlank
    private String clientId;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String grantType;

    @NotBlank
    private String clientSecret;

}
