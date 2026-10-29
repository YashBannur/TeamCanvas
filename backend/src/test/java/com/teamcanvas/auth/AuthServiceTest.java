package com.teamcanvas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamcanvas.auth.dto.LoginRequest;
import com.teamcanvas.auth.dto.RegisterRequest;
import com.teamcanvas.common.exception.ConflictException;
import com.teamcanvas.common.exception.UnauthorizedException;
import com.teamcanvas.config.AppProperties;
import com.teamcanvas.security.JwtService;
import com.teamcanvas.user.User;
import com.teamcanvas.user.UserRepository;
import com.teamcanvas.user.dto.UserResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit test of the business rules in AuthService. Repositories and the password encoder are mocked. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("unit-test-secret-that-is-at-least-32-characters-long", Duration.ofMinutes(30)),
                new AppProperties.Refresh(Duration.ofHours(24), "tc_refresh", false, "Lax"),
                new AppProperties.Cors(List.of("http://localhost:5173")));
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                authenticationManager, new JwtService(properties), properties);
    }

    @Test
    void register_normalisesEmail_andStoresOnlyTheHashedPassword() {
        when(userRepository.existsByEmail("yash@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("  Yash ", "  Yash@Example.COM ", "Password1"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("Yash", saved.getValue().getName());
        assertEquals("yash@example.com", saved.getValue().getEmail());
        assertEquals("hashed-password", saved.getValue().getPasswordHash());
        assertEquals("yash@example.com", response.email());
    }

    @Test
    void register_duplicateEmail_throwsConflict_andSavesNothing() {
        when(userRepository.existsByEmail("yash@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.register(new RegisterRequest("Yash", "yash@example.com", "Password1")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_badCredentials_throwsUnauthorizedWithGenericMessage() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("yash@example.com", "wrong")));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void refresh_validToken_revokesOldToken_andIssuesNewOne() {
        User user = new User("Yash", "yash@example.com", "hash");
        String raw = "raw-refresh-token";
        RefreshToken stored = new RefreshToken(user, TokenUtil.sha256Hex(raw), Instant.now().plusSeconds(600));
        when(refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex(raw))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResult result = authService.refresh(raw);

        assertTrue(stored.isRevoked(), "the used token must be revoked (rotation)");
        assertNotEquals(raw, result.refreshToken());
        assertNotNull(result.response().accessToken());
    }

    @Test
    void refresh_revokedToken_isRejected() {
        User user = new User("Yash", "yash@example.com", "hash");
        RefreshToken stored = new RefreshToken(user, TokenUtil.sha256Hex("raw"), Instant.now().plusSeconds(600));
        stored.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex("raw"))).thenReturn(Optional.of(stored));

        assertThrows(UnauthorizedException.class, () -> authService.refresh("raw"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_expiredToken_isRejected() {
        User user = new User("Yash", "yash@example.com", "hash");
        RefreshToken stored = new RefreshToken(user, TokenUtil.sha256Hex("raw"), Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex("raw"))).thenReturn(Optional.of(stored));

        assertThrows(UnauthorizedException.class, () -> authService.refresh("raw"));
    }

    @Test
    void refresh_unknownOrMissingToken_isRejected() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refresh("unknown"));
        assertThrows(UnauthorizedException.class, () -> authService.refresh(null));
        assertThrows(UnauthorizedException.class, () -> authService.refresh("  "));
    }
}
