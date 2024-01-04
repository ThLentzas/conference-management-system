package com.example.conference_management_system.user;

import com.example.conference_management_system.exception.ResourceNotFoundException;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.dto.UserDTO;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.exception.DuplicateResourceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";
    private static final UserDTOMapper dtoMapper = new UserDTOMapper();

    public void registerUser(User user) {
        if (this.userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
            throw new DuplicateResourceException("The provided username already exists");
        }

        this.userRepository.save(user);
    }

    UserDTO findUserByUsername(String username) {
        User user = this.userRepository.findUserByUsername(username).orElseThrow(() -> {
            logger.error("No user found with username: {}", username);

            return new ResourceNotFoundException("User not found with username: " + username);
        });

        return dtoMapper.apply(user);
    }

    UserDTO findUserById() {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = this.userRepository.findById(securityUser.user().getId()).orElseThrow(() ->
                new ResourceNotFoundException(USER_NOT_FOUND_MSG + securityUser.user().getId()));

        logger.info("User retrieved with id: {}", user.getId());

        return dtoMapper.apply(user);
    }

    public void validateUser(User user) {
        if (user.getUsername().length() > 20) {
            throw new IllegalArgumentException("Invalid username. Username must not exceed 20 characters");
        }

        if (user.getFullName().length() > 50) {
            throw new IllegalArgumentException("Invalid full name. Full name must not exceed 50 characters");
        }

        if (!user.getFullName().matches("^[a-zA-Z ]*$")) {
            throw new IllegalArgumentException("Invalid full name. Full name should contain only characters and spaces");
        }

        PasswordValidator validator = new org.passay.PasswordValidator(
                new LengthRule(12, 128),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1)
        );

        RuleResult result = validator.validate(new PasswordData(user.getPassword()));
        if (!result.isValid()) {
            throw new IllegalArgumentException(validator.getMessages(result).get(0));
        }
    }
}
