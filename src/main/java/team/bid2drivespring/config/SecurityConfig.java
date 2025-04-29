package team.bid2drivespring.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import team.bid2drivespring.model.User;
import team.bid2drivespring.service.*;

@Configuration
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private TwoFactorAuthenticationFilter twoFactorAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/register", "/login", "/activate",  "/createAdmin","/createAdmin/**", "/css/**", "/generateTokenForPasswordRecovery", "/passwordRecovery", "/js/**", "/logo.png", "/DefaultProfilePic.jpg", "/car-list.json").permitAll()
                        .requestMatchers("/administrator/**").hasRole("ADMIN")
                        .requestMatchers("/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/profileSettings", true)
                        .permitAll()
                        .failureHandler(authenticationFailureHandler())
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/profileSettings", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                );

        http.addFilterBefore(twoFactorAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService);
        authenticationManagerBuilder.authenticationProvider(new CustomAuthenticationProvider());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (exception instanceof LockedException) {
                request.getSession().setAttribute("error", exception.getMessage());
            } else if (exception instanceof BadCredentialsException) {
                String errorMessage = exception.getMessage();
                if ("User not found!".equals(errorMessage)) { request.getSession().setAttribute("error", "User not found! Please check your username."); }
                else { request.getSession().setAttribute("error", "Invalid username or password."); }
            }
            response.sendRedirect("/login?error=true");
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String username = authentication.getName();
            User user = userService.getCurrentUser(username);

            if (user.getRole() == User.Role.ADMIN) {
                response.sendRedirect("/administrator/users/pendingVerification");
            } else {
                if (user.isTwoFactorEnabled()) {
                    String twoFactorCode = userService.generateTwoFactorCode();
                    user.setTwoFactorCode(twoFactorCode);
                    userService.save(user);

                    emailService.sendTwoFactorCode(user.getEmail(), twoFactorCode);

                    response.sendRedirect("/2fa");
                } else {
                    response.sendRedirect("/profileSettings");
                }
            }
        };
    }

}

