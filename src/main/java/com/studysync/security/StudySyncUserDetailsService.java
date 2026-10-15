package com.studysync.security;

import com.studysync.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StudySyncUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public StudySyncUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(StudySyncUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account for this email"));
    }
}
