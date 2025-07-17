package com.example.techprototype.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * RSA密钥生成工具
 * 可以用于生成新的密钥对或者查看现有密钥
 */
public class KeyGenerator {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String KEYS_DIR = "keys";
    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法:");
            System.out.println("  generate - 生成新的密钥对");
            System.out.println("  view - 查看现有公钥");
            return;
        }

        String command = args[0];
        switch (command) {
            case "generate":
                generateKeys();
                break;
            case "view":
                viewPublicKey();
                break;
            default:
                System.out.println("未知命令: " + command);
                break;
        }
    }

    /**
     * 生成新的RSA密钥对
     */
    private static void generateKeys() {
        try {
            // 确保目录存在
            Files.createDirectories(Paths.get(KEYS_DIR));

            // 生成密钥对
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            // 保存私钥
            String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            try (FileWriter privateKeyWriter = new FileWriter(Paths.get(KEYS_DIR, PRIVATE_KEY_FILE).toString())) {
                privateKeyWriter.write(privateKeyBase64);
            }

            // 保存公钥
            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            try (FileWriter publicKeyWriter = new FileWriter(Paths.get(KEYS_DIR, PUBLIC_KEY_FILE).toString())) {
                publicKeyWriter.write(publicKeyBase64);
            }

            System.out.println("密钥对已成功生成并保存到 " + KEYS_DIR + " 目录");
            System.out.println("公钥:");
            System.out.println(publicKeyBase64);

        } catch (Exception e) {
            System.err.println("生成密钥对时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 查看现有公钥
     */
    private static void viewPublicKey() {
        try {
            String publicKeyPath = Paths.get(KEYS_DIR, PUBLIC_KEY_FILE).toString();
            if (!Files.exists(Paths.get(publicKeyPath))) {
                System.out.println("公钥文件不存在: " + publicKeyPath);
                return;
            }

            String publicKey = Files.readString(Paths.get(publicKeyPath));
            System.out.println("当前公钥:");
            System.out.println(publicKey);

        } catch (IOException e) {
            System.err.println("读取公钥时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
