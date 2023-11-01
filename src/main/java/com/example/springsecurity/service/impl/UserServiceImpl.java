package com.example.springsecurity.service.impl;

import com.example.springsecurity.dto.UserDTO;
import com.example.springsecurity.entities.Confirmation;
import com.example.springsecurity.entities.Role;
import com.example.springsecurity.entities.User;
import com.example.springsecurity.exception.ExistEmailException;
import com.example.springsecurity.exception.ExistUsernameException;
import com.example.springsecurity.reposiroty.ConfirmationRepository;
import com.example.springsecurity.reposiroty.RoleRepository;
import com.example.springsecurity.reposiroty.UserRepository;
import com.example.springsecurity.service.UserService;
import com.example.springsecurity.utils.ERole;
import com.example.springsecurity.utils.JwtUtils;
import com.example.springsecurity.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final JwtUtils jwtUtils;

    private final JavaMailSender javaMailSender;

    private final ConfirmationRepository confirmationRepository;


    @Override
    public User saveUser(UserDTO userDTO) {
        try {
            if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
                throw new ExistEmailException();
            }
            User user = new User();
            user.setEmail(userDTO.getEmail());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setStatus(false);
            user.setCreateAt(Instant.now());

            Role role = roleRepository.findByRoleName(ERole.ROLE_USER).orElseThrow(Exception::new);
            user.setRoles(Set.of(role));

            userRepository.save(user);

            Confirmation confirmation = new Confirmation(user);
            confirmationRepository.save(confirmation);

            /*TODO send email to verify account */



            return user;
        } catch (ExistEmailException e) {
            System.out.println(e.getMessage());
        } catch (ExistUsernameException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private void sendEmail(){

    }

    @Override
    public boolean verifyToken(String token) {
        if (confirmationRepository.existsByToken(token)) {
            Confirmation confirmation = confirmationRepository.findByToken(token);
            User user = userRepository.findByEmail(confirmation.getUser().getEmail()).orElseThrow(() -> new UsernameNotFoundException("Account not found"));
            user.setStatus(true);
            userRepository.save(user);
        }
        return false;
    }

    @Override
    public String auth(UserDTO userDTO) {
        byte[] bytePassword = Base64.getDecoder().decode(userDTO.getPassword());
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(userDTO.getEmail(), new String(bytePassword)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("Login : " + SecurityContextHolder.getContext().getAuthentication().getName());
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwtUtils.generateToken(userDetails.getUsername());
    }

    @Override
    public User userInfo(String brearToken) {
        String email = jwtUtils.getUsername(Utils.getToken(brearToken));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found !"));
        return user;
    }
}
