package ci.ashamaz.languageflash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .antMatchers("/admin/**").hasRole("ADMIN")
                                .antMatchers("/", "/css/**", "/js/**", "/auth/register", "/auth/reset-password/**",
                                        "/auth/confirm-email").permitAll()
                                .antMatchers("/texts").permitAll()
                                .antMatchers("/texts/add", "/texts/edit", "/texts/delete/**").hasRole("ADMIN")
                                .antMatchers("/texts/{id}").permitAll()
                                .antMatchers("/about", "/method", "/contacts").permitAll()
                                // API endpoints for authenticated users
                                .antMatchers("/api/dashboard/**").authenticated()
                                .antMatchers("/api/learn/**").authenticated()
                                .antMatchers("/api/admin/**").hasRole("ADMIN")

                                .anyRequest().authenticated()
                )
                .formLogin()
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error")
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll();
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}