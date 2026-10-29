package com.teamcanvas.auth;

import com.teamcanvas.auth.dto.AuthResponse;
import com.teamcanvas.auth.dto.LoginRequest;
import com.teamcanvas.auth.dto.RegisterRequest;
import com.teamcanvas.common.exception.ConflictException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.common.exception.UnauthorizedException;
import com.teamcanvas.common.util.Emails;
import com.teamcanvas.config.AppProperties;
import com.teamcanvas.security.JwtService;
import com.teamcanvas.security.UserPrincipal;
import com.teamcanvas.user.User;
import com.teamcanvas.user.UserRepository;
import com.teamcanvas.user.dto.UserResponse;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login, refresh (with token rotation) and logout.
 *
 * Token model:
 *  - access token: short-lived JWT, sent by the client in the Authorization header.
 *  - refresh token: random opaque value in an HttpOnly cookie; only its hash is stored. Every refresh revokes the
 *    old token and issues a new one with a fresh lifetime (a sliding session).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppProperties properties;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AppProperties properties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = Emails.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(Emails.normalize(request.email()), request.password()));
        } catch (AuthenticationException ex) {
            // Same message for "unknown email" and "wrong password" so attackers cannot discover which emails exist.
            throw new UnauthorizedException("Invalid email or password");
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is missing");
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));
        if (stored.isRevoked() || stored.isExpired()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }
        // Rotation: a refresh token can be used exactly once.
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex(rawRefreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private AuthResult issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(UserPrincipal.from(user));

        String rawRefreshToken = TokenUtil.generateRawToken();
        Instant expiresAt = Instant.now().plus(properties.refresh().expiry());
        refreshTokenRepository.save(new RefreshToken(user, TokenUtil.sha256Hex(rawRefreshToken), expiresAt));

        AuthResponse response = new AuthResponse(
                accessToken, "Bearer", jwtService.getAccessTokenExpirySeconds(), UserResponse.from(user));
        return new AuthResult(response, rawRefreshToken);
    }
}
