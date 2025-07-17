package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResponseTest {

    @Test
    void getStatus() {
        ProfileResponse response = new ProfileResponse();
        response.setStatus("SUCCESS");
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void getMessage() {
        ProfileResponse response = new ProfileResponse();
        response.setMessage("获取成功");
        assertEquals("获取成功", response.getMessage());
    }

    @Test
    void getProfile() {
        ProfileResponse.UserProfileData profileData = new ProfileResponse.UserProfileData();
        ProfileResponse response = new ProfileResponse();
        response.setProfile(profileData);
        assertEquals(profileData, response.getProfile());
    }

    @Test
    void getTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        ProfileResponse response = new ProfileResponse();
        response.setTimestamp(now);
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void setStatus() {
        ProfileResponse response = new ProfileResponse();
        response.setStatus("FAILURE");
        assertEquals("FAILURE", response.getStatus());
    }

    @Test
    void setMessage() {
        ProfileResponse response = new ProfileResponse();
        response.setMessage("操作失败");
        assertEquals("操作失败", response.getMessage());
    }

    @Test
    void setProfile() {
        ProfileResponse.UserProfileData profileData = new ProfileResponse.UserProfileData();
        profileData.setUserId(1L);

        ProfileResponse response = new ProfileResponse();
        response.setProfile(profileData);

        assertEquals(profileData, response.getProfile());
        assertEquals(1L, response.getProfile().getUserId());
    }

    @Test
    void setTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        ProfileResponse response = new ProfileResponse();
        response.setTimestamp(now);
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testEquals() {
        LocalDateTime now = LocalDateTime.now();
        ProfileResponse.UserProfileData profile1 = new ProfileResponse.UserProfileData(1L, "张三", "test@example.com",
                "13812345678", now, now, (byte) 1, 2, null);

        ProfileResponse response1 = new ProfileResponse("SUCCESS", "成功", profile1, now);
        ProfileResponse response2 = new ProfileResponse("SUCCESS", "成功", profile1, now);
        ProfileResponse response3 = new ProfileResponse("FAILURE", "失败", null, now);

        assertTrue(response1.equals(response2));
        assertTrue(response2.equals(response1));
        assertFalse(response1.equals(response3));
        assertFalse(response1.equals(null));
        assertTrue(response1.equals(response1));
    }

    @Test
    void canEqual() {
        ProfileResponse response1 = new ProfileResponse();
        ProfileResponse response2 = new ProfileResponse();
        Object otherObject = new Object();

        assertTrue(response1.canEqual(response2));
        assertFalse(response1.canEqual(otherObject));
    }

    @Test
    void testHashCode() {
        LocalDateTime now = LocalDateTime.now();
        ProfileResponse.UserProfileData profile = new ProfileResponse.UserProfileData(1L, "张三", "test@example.com",
                "13812345678", now, now, (byte) 1, 2, null);

        ProfileResponse response1 = new ProfileResponse("SUCCESS", "成功", profile, now);
        ProfileResponse response2 = new ProfileResponse("SUCCESS", "成功", profile, now);
        ProfileResponse response3 = new ProfileResponse("FAILURE", "失败", null, now);

        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testToString() {
        LocalDateTime now = LocalDateTime.now();
        ProfileResponse.UserProfileData profile = new ProfileResponse.UserProfileData(1L, "张三", "test@example.com",
                "13812345678", now, now, (byte) 1, 2, null);

        ProfileResponse response = new ProfileResponse("SUCCESS", "成功", profile, now);
        String toString = response.toString();

        assertTrue(toString.contains("status=SUCCESS"));
        assertTrue(toString.contains("message=成功"));
        assertTrue(toString.contains("profile="));
        assertTrue(toString.contains("timestamp="));
    }

    @Test
    void testSuccessFactoryMethod() {
        LocalDateTime now = LocalDateTime.now();
        ProfileResponse.UserProfileData profile = new ProfileResponse.UserProfileData(1L, "张三", "test@example.com",
                "13812345678", now, now, (byte) 1, 2, null);

        ProfileResponse response = ProfileResponse.success(profile);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("获取成功", response.getMessage());
        assertEquals(profile, response.getProfile());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testFailureFactoryMethod() {
        String errorMessage = "用户不存在";
        ProfileResponse response = ProfileResponse.failure(errorMessage);

        assertEquals("FAILURE", response.getStatus());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getProfile());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testUserProfileData() {
        LocalDateTime now = LocalDateTime.now();
        List<ProfileResponse.PassengerData> passengers = Arrays.asList(
                new ProfileResponse.PassengerData(1L, "320123199001011234", "张三", (byte) 1, "成人", "13812345678", 0)
        );

        ProfileResponse.UserProfileData profile = new ProfileResponse.UserProfileData();
        profile.setUserId(1L);
        profile.setRealName("张三");
        profile.setEmail("test@example.com");
        profile.setPhoneNumber("13812345678");
        profile.setRegistrationTime(now);
        profile.setLastLoginTime(now);
        profile.setAccountStatus((byte) 1);
        profile.setRelatedPassenger(1);
        profile.setLinkedPassengers(passengers);

        assertEquals(1L, profile.getUserId());
        assertEquals("张三", profile.getRealName());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("13812345678", profile.getPhoneNumber());
        assertEquals(now, profile.getRegistrationTime());
        assertEquals(now, profile.getLastLoginTime());
        assertEquals((byte) 1, profile.getAccountStatus());
        assertEquals(1, profile.getRelatedPassenger());
        assertEquals(passengers, profile.getLinkedPassengers());
    }

    @Test
    void testPassengerData() {
        ProfileResponse.PassengerData passenger = new ProfileResponse.PassengerData();
        passenger.setPassengerId(1L);
        passenger.setIdCardNumber("320123199001011234");
        passenger.setRealName("张三");
        passenger.setPassengerType((byte) 1);
        passenger.setPassengerTypeText("成人");
        passenger.setPhoneNumber("13812345678");
        passenger.setStudentTypeLeft(0);

        assertEquals(1L, passenger.getPassengerId());
        assertEquals("320123199001011234", passenger.getIdCardNumber());
        assertEquals("张三", passenger.getRealName());
        assertEquals((byte) 1, passenger.getPassengerType());
        assertEquals("成人", passenger.getPassengerTypeText());
        assertEquals("13812345678", passenger.getPhoneNumber());
        assertEquals(0, passenger.getStudentTypeLeft());
    }
}