package com.redsocial.ui.session;

import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.UserDto;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@VaadinSessionScope
public class UserSession implements Serializable {

    private String token;
    private UserDto user;

    public boolean isLoggedIn() {
        return token != null && user != null;
    }

    public void setAuth(AuthResponse response) {
        this.token = response.token();
        this.user = response.user();
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserDto getUser() {
        return user;
    }

    public void logout() {
        this.token = null;
        this.user = null;
    }
}
