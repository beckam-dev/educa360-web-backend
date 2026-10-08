package com.educa360.backend.security;

import com.educa360.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Puente entre nuestro User (JPA) y el modelo de seguridad de Spring.
 *
 * - authorities: "ROLE_" + rol  → alimenta a @PreAuthorize("hasRole('...')")
 * - disabled:    !User.activo   → un usuario dado de baja no puede iniciar sesión
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.educa360.backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe una cuenta con el email " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRol().name())
                .disabled(!user.isActivo())
                .build();
    }
}
