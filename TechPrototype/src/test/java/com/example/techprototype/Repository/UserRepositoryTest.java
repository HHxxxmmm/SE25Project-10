package com.example.techprototype.Repository;

import com.example.techprototype.Entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setRealName("Test User");
        testUser.setPasswordHash("hashedPassword123");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("13800138000");
        testUser.setRegistrationTime(LocalDateTime.now());
        testUser.setAccountStatus((byte)1);
        testUser.setRelatedPassenger(0);

        // 保存测试用户到数据库
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        entityManager.clear();
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        // when
        Optional<User> found = userRepository.findByEmail(testUser.getEmail());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(found.get().getRealName()).isEqualTo(testUser.getRealName());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        // when
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void findByPhoneNumber_ShouldReturnUser_WhenPhoneNumberExists() {
        // when
        Optional<User> found = userRepository.findByPhoneNumber(testUser.getPhoneNumber());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getPhoneNumber()).isEqualTo(testUser.getPhoneNumber());
        assertThat(found.get().getRealName()).isEqualTo(testUser.getRealName());
    }

    @Test
    void findByPhoneNumber_ShouldReturnEmpty_WhenPhoneNumberDoesNotExist() {
        // when
        Optional<User> found = userRepository.findByPhoneNumber("13900139000");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        // when
        boolean exists = userRepository.existsByEmail(testUser.getEmail());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        // when
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByPhoneNumber_ShouldReturnTrue_WhenPhoneNumberExists() {
        // when
        boolean exists = userRepository.existsByPhoneNumber(testUser.getPhoneNumber());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByPhoneNumber_ShouldReturnFalse_WhenPhoneNumberDoesNotExist() {
        // when
        boolean exists = userRepository.existsByPhoneNumber("13900139000");

        // then
        assertThat(exists).isFalse();
    }
}