package com.verse.store.shared.security;

import java.util.List;

public record ProfileViewModel(
        String name,
        String username,
        String email,
        List<String> roles) {
}
