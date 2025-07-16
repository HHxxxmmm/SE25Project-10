package com.example.techprototype.Util;

import org.springframework.stereotype.Component;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密和签名工具类
 */
@Component
public class CryptoUtil {

    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    // 使用更小的密钥长度来减少签名长度
    private static final int KEY_SIZE = 1024;

    /**
     * 生成RSA密钥对
     *
     * @return KeyPair对象
     */
    public KeyPair generateKeyPair() {
        try {
            System.out.println("开始生成RSA密钥对 - 算法: " + KEY_ALGORITHM + ", 密钥大小: " + KEY_SIZE + " bits");
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            System.out.println("密钥对生成成功 - 公钥长度: " + keyPair.getPublic().getEncoded().length + 
                             ", 私钥长度: " + keyPair.getPrivate().getEncoded().length);
            return keyPair;
        } catch (Exception e) {
            System.err.println("生成密钥对失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("生成密钥对失败", e);
        }
    }

    /**
     * 使用私钥签名数据
     *
     * @param data 待签名数据
     * @param privateKey 私钥
     * @return 签名结果的Base64编码
     */
    public String sign(byte[] data, PrivateKey privateKey) {
        try {
            System.out.println("开始签名 - 数据长度: " + data.length);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            String result = Base64.getEncoder().encodeToString(signature.sign());
            System.out.println("签名完成 - 签名结果长度: " + result.length());
            return result;
        } catch (Exception e) {
            System.err.println("签名数据失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("签名数据失败", e);
        }
    }

    /**
     * 验证签名
     *
     * @param data 原始数据
     * @param signature 签名的Base64编码
     * @param publicKey 公钥
     * @return 验证结果
     */
    public boolean verify(byte[] data, String signature, PublicKey publicKey) {
        try {
            System.out.println("开始验证签名 - 数据长度: " + data.length + ", 签名长度: " + signature.length());
            
            // 检查签名格式
            if (!signature.matches("^[A-Za-z0-9+/]+={0,2}$")) {
                System.err.println("警告: 签名不是有效的Base64格式");
            }
            
            // 解码签名
            byte[] signatureBytes;
            try {
                signatureBytes = Base64.getDecoder().decode(signature);
                System.out.println("解码后的签名字节长度: " + signatureBytes.length);
            } catch (IllegalArgumentException e) {
                System.err.println("Base64解码失败: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            
            // 验证签名
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(data);
            boolean result = verifier.verify(signatureBytes);
            
            System.out.println("验证结果: " + (result ? "成功" : "失败"));
            return result;
        } catch (Exception e) {
            System.err.println("验证签名过程异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("验证签名失败", e);
        }
    }

    /**
     * 将私钥转换为Base64编码的字符串
     *
     * @param privateKey 私钥
     * @return Base64编码的私钥字符串
     */
    public String privateKeyToBase64(PrivateKey privateKey) {
        System.out.println("正在将私钥转换为Base64编码");
        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        System.out.println("私钥Base64编码完成，长度: " + base64Key.length());
        return base64Key;
    }

    /**
     * 将公钥转换为Base64编码的字符串
     *
     * @param publicKey 公钥
     * @return Base64编码的公钥字符串
     */
    public String publicKeyToBase64(PublicKey publicKey) {
        System.out.println("正在将公钥转换为Base64编码");
        String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        System.out.println("公钥Base64编码完成，长度: " + base64Key.length());
        return base64Key;
    }

    /**
     * 从Base64编码的字符串还原私钥
     *
     * @param base64PrivateKey Base64编码的私钥字符串
     * @return 私钥对象
     */
    public PrivateKey base64ToPrivateKey(String base64PrivateKey) {
        try {
            System.out.println("从Base64字符串还原私钥，输入长度: " + base64PrivateKey.length());
            
            // 验证Base64格式
            if (!base64PrivateKey.matches("^[A-Za-z0-9+/]+={0,2}$")) {
                System.err.println("警告: 输入的私钥字符串不是有效的Base64格式");
            }
            
            byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
            System.out.println("Base64解码成功，得到字节数: " + keyBytes.length);
            
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            System.out.println("私钥还原成功，算法: " + privateKey.getAlgorithm());
            
            return privateKey;
        } catch (Exception e) {
            System.err.println("从Base64还原私钥失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("从Base64还原私钥失败", e);
        }
    }

    /**
     * 从Base64编码的字符串还原公钥
     *
     * @param base64PublicKey Base64编码的公钥字符串
     * @return 公钥对象
     */
    public PublicKey base64ToPublicKey(String base64PublicKey) {
        try {
            System.out.println("从Base64字符串还原公钥，输入长度: " + base64PublicKey.length());
            
            // 验证Base64格式
            if (!base64PublicKey.matches("^[A-Za-z0-9+/]+={0,2}$")) {
                System.err.println("警告: 输入的公钥字符串不是有效的Base64格式");
            }
            
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            System.out.println("Base64解码成功，得到字节数: " + keyBytes.length);
            
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            System.out.println("公钥还原成功，算法: " + publicKey.getAlgorithm());
            
            return publicKey;
        } catch (Exception e) {
            System.err.println("从Base64还原公钥失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("从Base64还原公钥失败", e);
        }
    }
}
