package com.github.dmitriims.posikengine.dto.userprovaideddata;

import lombok.Data;

@Data
public class AuthDetails {
    private String username;
    private String password;
    private String role;
}
