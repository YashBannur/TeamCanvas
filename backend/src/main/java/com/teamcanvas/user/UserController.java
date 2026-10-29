package com.teamcanvas.user;

import com.teamcanvas.security.UserPrincipal;
import com.teamcanvas.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Returns the currently authenticated user. The id comes from the verified JWT, never from the request. */
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getById(principal.getId());
    }
}
