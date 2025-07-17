package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.PassengerDAO;
import com.example.techprototype.DAO.UserDAO;
import com.example.techprototype.DAO.UserPassengerRelationDAO;
import com.example.techprototype.DTO.LoginDTO;
import com.example.techprototype.DTO.RegisterDTO;
import com.example.techprototype.DTO.UserDTO;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Spy;

public class UserServiceImplTest {

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserDAO userDAO;

    @Mock
    private PassengerDAO passengerDAO;

    @Mock
    private UserPassengerRelationDAO userPassengerRelationDAO;

    @Mock
    private JwtUtil jwtUtil;

    private User mockUser;
    private Passenger mockPassenger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // 使用@Spy和@InjectMocks自动注入依赖和创建spy

        // 创建模拟用户
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setRealName("测试用户");
        mockUser.setPhoneNumber("13800138000");
        mockUser.setPasswordHash("5f4dcc3b5aa765d61d8327deb882cf99"); // 简化的MD5密码
        mockUser.setEmail("test@example.com");
        mockUser.setAccountStatus((byte) 1);
        mockUser.setRegistrationTime(LocalDateTime.now());
        mockUser.setLastLoginTime(LocalDateTime.now());
        mockUser.setPassengerId(1L);
        mockUser.setRelatedPassenger(1);

        // 创建模拟乘客
        mockPassenger = new Passenger();
        mockPassenger.setPassengerId(1L);
        mockPassenger.setRealName("测试用户");
        mockPassenger.setIdCardNumber("110101199001010011");
        mockPassenger.setPhoneNumber("13800138000");
        mockPassenger.setPassengerType((byte) 1);
    }

    @Test
    @DisplayName("登录成功测试")
    public void testLoginSuccess() throws Exception {
        // 准备测试数据
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setPhoneNumber("13800138000");
        loginDTO.setPassword("password");

        // 模拟UserDAO查询返回
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.of(mockUser));

        // 模拟JwtUtil生成token
        when(jwtUtil.generateToken(anyLong())).thenReturn("mock-jwt-token");

        // 模拟MD5密码生成，确保使用正确的doReturn...when语法
        doReturn("5f4dcc3b5aa765d61d8327deb882cf99").when(userService).generateMD5Password(anyString());

        // 执行测试
        UserDTO result = userService.login(loginDTO);

        // 验证结果
        assertNotNull(result, "登录应该返回用户信息");
        assertEquals(mockUser.getUserId(), result.getUserId(), "用户ID应该匹配");
        assertEquals(mockUser.getRealName(), result.getRealName(), "用户名应该匹配");
        assertEquals(mockUser.getPhoneNumber(), result.getPhoneNumber(), "手机号应该匹配");
        assertEquals("mock-jwt-token", result.getToken(), "应该有JWT令牌");

        // 验证交互
        verify(userDAO).update(any(User.class)); // 验证最后登录时间已更新
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    public void testLoginUserNotExists() {
        // 准备测试数据
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setPhoneNumber("13900139000");
        loginDTO.setPassword("password");

        // 模拟UserDAO查询返回空
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        // 执行测试并验证异常
        Exception exception = assertThrows(Exception.class, () -> {
            userService.login(loginDTO);
        });

        assertEquals("用户不存在", exception.getMessage(), "应该抛出用户不存在异常");
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    public void testLoginPasswordIncorrect() {
        // 准备测试数据
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setPhoneNumber("13800138000");
        loginDTO.setPassword("wrong-password");

        // 模拟UserDAO查询返回
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.of(mockUser));

        // 模拟MD5密码生成返回不匹配结果，使用doReturn...when语法而不是when...thenReturn
        doReturn("incorrect-hash").when(userService).generateMD5Password(anyString());

        // 执行测试并验证异常
        Exception exception = assertThrows(Exception.class, () -> {
            userService.login(loginDTO);
        });

        assertEquals("密码错误", exception.getMessage(), "应该抛出密码错误异常");
    }

    @Test
    @DisplayName("注册成功测试")
    public void testRegisterSuccess() throws Exception {
        // 准备测试数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setRealName("新用户");
        registerDTO.setPhoneNumber("13900139000");
        registerDTO.setPassword("password");
        registerDTO.setEmail("new@example.com");
        registerDTO.setIdCardNumber("110101199001010011");

        // 模拟UserDAO查询返回（手机号和邮箱不存在）
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(userDAO.findByEmail(anyString())).thenReturn(Optional.empty());

        // 模拟PassengerDAO查询返回（找不到乘客，需要创建新乘客）
        when(passengerDAO.findByIdCardNumber(anyString())).thenReturn(Optional.empty());

        // 模拟数据库保存操作
        when(passengerDAO.save(any(Passenger.class))).thenAnswer(invocation -> {
            Passenger passenger = invocation.getArgument(0);
            passenger.setPassengerId(2L);
            return passenger;
        });

        when(userDAO.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(2L);
            return user;
        });

        // 模拟JwtUtil生成token
        when(jwtUtil.generateToken(anyLong())).thenReturn("mock-jwt-token");

        // 模拟密码加密
        doReturn("hashed-password").when(userService).generateMD5Password(anyString());

        // 执行测试
        UserDTO result = userService.register(registerDTO);

        // 验证结果
        assertNotNull(result, "注册应该返回用户信息");
        assertEquals(2L, result.getUserId(), "用户ID应该匹配");
        assertEquals(registerDTO.getRealName(), result.getRealName(), "用户名应该匹配");
        assertEquals(registerDTO.getPhoneNumber(), result.getPhoneNumber(), "手机号应该匹配");
        assertEquals(registerDTO.getEmail(), result.getEmail(), "邮箱应该匹配");
        assertEquals("mock-jwt-token", result.getToken(), "应该有JWT令牌");

        // 验证调用
        verify(userPassengerRelationDAO).save(any());
    }

    @Test
    @DisplayName("注册失败 - 手机号已存在")
    public void testRegisterPhoneNumberExists() {
        // 准备测试数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setRealName("新用户");
        registerDTO.setPhoneNumber("13800138000");
        registerDTO.setPassword("password");
        registerDTO.setEmail("new@example.com");
        registerDTO.setIdCardNumber("110101199001010011");

        // 模拟UserDAO查询返回（手机号已存在）
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.of(mockUser));

        // 执行测试并验证异常
        Exception exception = assertThrows(Exception.class, () -> {
            userService.register(registerDTO);
        });

        assertEquals("手机号已被注册", exception.getMessage(), "应该抛出手机号已注册异常");
    }

    @Test
    @DisplayName("注册失败 - 邮箱已存在")
    public void testRegisterEmailExists() {
        // 准备测试数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setRealName("新用户");
        registerDTO.setPhoneNumber("13900139000");
        registerDTO.setPassword("password");
        registerDTO.setEmail("test@example.com");
        registerDTO.setIdCardNumber("110101199001010011");

        // 模拟UserDAO查询返回（手机号不存在，但邮箱存在）
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(userDAO.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        // 执行测试并验证异常
        Exception exception = assertThrows(Exception.class, () -> {
            userService.register(registerDTO);
        });

        assertEquals("邮箱已被注册", exception.getMessage(), "应该抛出邮箱已注册异常");
    }

    @Test
    @DisplayName("注册失败 - 身份证号无效")
    public void testRegisterInvalidIdCard() {
        // 准备测试数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setRealName("新用户");
        registerDTO.setPhoneNumber("13900139000");
        registerDTO.setPassword("password");
        registerDTO.setEmail("new@example.com");
        registerDTO.setIdCardNumber("invalid-id-card");

        // 模拟UserDAO查询返回（手机号和邮箱不存在）
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(userDAO.findByEmail(anyString())).thenReturn(Optional.empty());

        // 执行测试并验证异常
        Exception exception = assertThrows(Exception.class, () -> {
            userService.register(registerDTO);
        });

        assertEquals("身份证号码不合法", exception.getMessage(), "应该抛出身份证号不合法异常");
    }

    @Test
    @DisplayName("验证身份证号合法性")
    public void testValidateIdCard() {
        // 有效的18位身份证号
        assertTrue(userService.validateIdCard("110101199001010011"), "有效的18位身份证号应该通过验证");

        // 有效的15位身份证号
        assertTrue(userService.validateIdCard("110101900101001"), "有效的15位身份证号应该通过验证");

        // 无效的身份证号
        assertFalse(userService.validateIdCard("123"), "短身份证号应该验证失败");
        assertFalse(userService.validateIdCard(null), "null身份证号应该验证失败");
        assertFalse(userService.validateIdCard("abcdefghijklmnopqr"), "非数字身份证号应该验证失败");
    }

    @Test
    @DisplayName("生成MD5密码")
    public void testGenerateMD5Password() {
        String password = "password";
        String md5Password = userService.generateMD5Password(password);

        assertNotNull(md5Password, "MD5密码不应该为空");
        assertEquals(32, md5Password.length(), "MD5密码应该是32位十六进制字符");
    }

    @Test
    @DisplayName("通过ID获取用户")
    public void testGetUserById() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userDAO.findById(999L)).thenReturn(Optional.empty());

        User result1 = userService.getUserById(1L);
        User result2 = userService.getUserById(999L);

        assertNotNull(result1, "应该找到已存在的用户");
        assertEquals(mockUser.getUserId(), result1.getUserId(), "用户ID应该匹配");

        assertNull(result2, "不存在的用户ID应该返回null");
    }

    @Test
    @DisplayName("通过手机号获取用户")
    public void testGetUserByPhoneNumber() {
        when(userDAO.findByPhoneNumber("13800138000")).thenReturn(Optional.of(mockUser));
        when(userDAO.findByPhoneNumber("13900139000")).thenReturn(Optional.empty());

        User result1 = userService.getUserByPhoneNumber("13800138000");
        User result2 = userService.getUserByPhoneNumber("13900139000");

        assertNotNull(result1, "应该找到已存在的用户");
        assertEquals(mockUser.getPhoneNumber(), result1.getPhoneNumber(), "用户手机号应该匹配");

        assertNull(result2, "不存在的手机号应该返回null");
    }

    @Test
    @DisplayName("BCrypt格式密码验证")
    public void testLoginWithBCryptPassword() throws Exception {
        // 准备测试数据
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setPhoneNumber("13800138000");
        loginDTO.setPassword("password");

        // 创建带BCrypt密码的用户
        User bcryptUser = new User();
        bcryptUser.setUserId(2L);
        bcryptUser.setRealName("BCrypt用户");
        bcryptUser.setPhoneNumber("13800138000");
        // 使用BCrypt格式的密码哈希
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        bcryptUser.setPasswordHash(passwordEncoder.encode("password"));
        bcryptUser.setEmail("bcrypt@example.com");
        bcryptUser.setAccountStatus((byte) 1);
        bcryptUser.setRegistrationTime(LocalDateTime.now());
        bcryptUser.setLastLoginTime(LocalDateTime.now());
        bcryptUser.setPassengerId(2L);
        bcryptUser.setRelatedPassenger(1);

        // 模拟UserDAO查询返回
        when(userDAO.findByPhoneNumber(anyString())).thenReturn(Optional.of(bcryptUser));

        // 模拟JwtUtil生成token
        when(jwtUtil.generateToken(anyLong())).thenReturn("mock-jwt-token");

        // 执行测试
        UserDTO result = userService.login(loginDTO);

        // 验证结果
        assertNotNull(result, "登录应该返回用户信息");
        assertEquals(bcryptUser.getUserId(), result.getUserId(), "用户ID应该匹配");
    }
}