package team.bid2drivespring.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import team.bid2drivespring.model.User;

import java.io.IOException;

@Component
public class TwoFactorAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getCurrentUser(authentication.getName());

            if (user.isTwoFactorEnabled() && user.getTwoFactorCode() != null) {
                if (!request.getRequestURI().equals("/2fa")) {
                    response.sendRedirect("/2fa");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
