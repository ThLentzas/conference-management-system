package com.example.conference_management_system.paper;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.PaperUser;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.role.RoleRepository;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.UserRepository;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaperUserRepositoryTest extends AbstractUnitTest {
    @Autowired
    private PaperRepository paperRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PaperUserRepository underTest;

    //existsByPaperIdUserIdAndRoleType()
    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = {"ROLE_AUTHOR", "ROLE_REVIEWER"})
    void shouldReturnTrueWhenUserIsOwnerOfPaper(RoleType roleType) {
        Paper paper = getPaper();
        User user = getUser();
        Role role = new Role(roleType);
        user.setRoles(Set.of(role));
        this.paperRepository.save(paper);
        this.roleRepository.save(role);
        this.userRepository.save(user);
        PaperUser paperUser = new PaperUser(
                new PaperUserId(paper.getId(), user.getId()),
                paper,
                user,
                roleType
        );
        this.underTest.save(paperUser);

        assertThat(this.underTest.existsByPaperIdUserIdAndRoleType(paper.getId(), user.getId(), roleType)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = {"ROLE_AUTHOR", "ROLE_REVIEWER"})
    void shouldReturnFalseWhenUserIsNotOwnerOfPaper(RoleType roleType) {
        Paper paper = getPaper();
        User user = getUser();
        Role role = new Role(roleType);
        user.setRoles(Set.of(role));
        this.paperRepository.save(paper);
        this.roleRepository.save(role);
        this.userRepository.save(user);
        PaperUser paperUser = new PaperUser(
                new PaperUserId(paper.getId(), user.getId()),
                paper,
                user,
                roleType
        );
        this.underTest.save(paperUser);

        assertThat(this.underTest.existsByPaperIdUserIdAndRoleType(paper.getId(), user.getId() + 1, roleType))
                .isFalse();
    }

    private User getUser() {
        User user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setFullName("full name");

        return user;
    }

    private Paper getPaper() {
        Paper paper = new Paper();
        paper.setTitle("title");
        paper.setAbstractText("abstract text");
        paper.setAuthors("author");
        paper.setKeywords("keyword");

        return paper;
    }
}
