package com.example.conference_management_system.user;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.stereotype.Service;

import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.exception.DuplicateResourceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User registerUser(User user) {
        if (this.userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
            throw new DuplicateResourceException("The provided username already exists");
        }

        return this.userRepository.save(user);
    }

    public void validateUser(User user) {
        if (user.getUsername().length() > 20) {
            throw new IllegalArgumentException("Invalid username. Username must not exceed 20 characters");
        }

        if (user.getFullName().length() > 50) {
            throw new IllegalArgumentException("Invalid full name. Full name must not exceed 50 characters");
        }

        if (!user.getFullName().matches("^[a-zA-Z]*$")) {
            throw new IllegalArgumentException("Invalid full name. Full name should contain only characters");
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
