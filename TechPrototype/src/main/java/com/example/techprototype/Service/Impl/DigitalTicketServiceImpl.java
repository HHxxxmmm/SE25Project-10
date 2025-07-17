package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.DigitalTicketData;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.Train;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Repository.TrainRepository;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Service.DigitalTicketService;
import com.example.techprototype.Util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class DigitalTicketServiceImpl implements DigitalTicketService {

    @Autowired
    private CryptoUtil cryptoUtil;
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Value("${app.keys.directory:keys}")
    private String keysDirectory;
    
    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";
    
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyBase64;
    
    // 使用更短的签名长度，最大不超过255个字符
    private static final int MAX_SIGNATURE_LENGTH = 255;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @PostConstruct
    public void init() {
        System.out.println("初始化数字票证服务 - 密钥目录: " + keysDirectory + 
                         ", 签名最大长度限制: " + MAX_SIGNATURE_LENGTH + " 字符");
        
        // 确保密钥目录存在
        File directory = new File(keysDirectory);
        if (!directory.exists()) {
            System.out.println("密钥目录不存在，正在创建目录: " + keysDirectory);
            directory.mkdirs();
        }

        // 尝试加载密钥
        try {
            loadKeys();
            System.out.println("成功加载现有RSA密钥");
        } catch (Exception e) {
            System.out.println("加载密钥失败，正在生成新的RSA密钥对: " + e.getMessage());
            // 如果加载失败，则生成新的密钥对
            generateAndSaveKeys();
        }
    }

    /**
     * 加载密钥
     */
    private void loadKeys() throws IOException {
        System.out.println("正在加载RSA密钥...");
        
        File privateKeyFile = new File(keysDirectory, PRIVATE_KEY_FILE);
        File publicKeyFile = new File(keysDirectory, PUBLIC_KEY_FILE);

        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            System.err.println("密钥文件不存在: " + 
                              "私钥文件存在=" + privateKeyFile.exists() + ", " +
                              "公钥文件存在=" + publicKeyFile.exists());
            throw new IOException("密钥文件不存在");
        }

        String privateKeyContent = Files.readString(privateKeyFile.toPath());
        String publicKeyContent = Files.readString(publicKeyFile.toPath());
        
        System.out.println("读取密钥文件成功 - 私钥长度: " + privateKeyContent.length() + 
                         ", 公钥长度: " + publicKeyContent.length());

        this.privateKey = cryptoUtil.base64ToPrivateKey(privateKeyContent);
        this.publicKey = cryptoUtil.base64ToPublicKey(publicKeyContent);
        this.publicKeyBase64 = publicKeyContent;
        
        System.out.println("密钥加载完成 - 算法: " + privateKey.getAlgorithm());
    }

    /**
     * 生成并保存密钥对
     */
    private void generateAndSaveKeys() {
        try {
            KeyPair keyPair = cryptoUtil.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();

            // 转换为Base64格式
            String privateKeyBase64 = cryptoUtil.privateKeyToBase64(privateKey);
            this.publicKeyBase64 = cryptoUtil.publicKeyToBase64(publicKey);

            // 保存到文件
            Files.writeString(Paths.get(keysDirectory, PRIVATE_KEY_FILE), privateKeyBase64);
            Files.writeString(Paths.get(keysDirectory, PUBLIC_KEY_FILE), this.publicKeyBase64);
            
        } catch (Exception e) {
            throw new RuntimeException("生成和保存密钥对失败", e);
        }
    }

    @Override
    public DigitalTicketData generateSignedTicketData(Ticket ticket) {
        System.out.println("开始生成票证签名数据 - 票证ID: " + ticket.getTicketId());
        
        // 获取必要的数据
        Optional<Passenger> passenger = passengerRepository.findById(ticket.getPassengerId());
        Optional<Train> train = trainRepository.findById(ticket.getTrainId());

        if (passenger.isEmpty() || train.isEmpty()) {
            System.err.println("生成票证签名失败 - 无法获取乘客或列车信息 - 票证ID: " + ticket.getTicketId() + 
                             ", 乘客ID: " + ticket.getPassengerId() + 
                             ", 列车ID: " + ticket.getTrainId());
            throw new RuntimeException("无法获取乘客或列车信息");
        }

        // 创建数字票证数据
        DigitalTicketData ticketData = DigitalTicketData.builder()
            .ticketId(ticket.getTicketId())
            .ticketNumber(ticket.getTicketNumber())
            .passengerId(ticket.getPassengerId())
            .passengerName(passenger.get().getRealName())
            .travelDate(ticket.getTravelDate().toString())
            .trainNumber(train.get().getTrainNumber())
            .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMAT))
            .build();

        System.out.println("票证基本数据准备完成 - 票证ID: " + ticket.getTicketId() + 
                         ", 票证号: " + ticket.getTicketNumber() + 
                         ", 乘客: " + passenger.get().getRealName() + 
                         ", 列车号: " + train.get().getTrainNumber());
        
        // 签名
        String dataToSign = ticketData.getDataToSign();
        System.out.println("准备签名的数据: " + dataToSign);
        
        // 原始签名
        String originalSignature = cryptoUtil.sign(dataToSign.getBytes(), privateKey);
        System.out.println("原始签名长度: " + originalSignature.length());
        
        // 处理签名使其适合数据库字段长度 (varchar(255))
        String signature = compressSignature(originalSignature);
        System.out.println("压缩后签名长度: " + signature.length());
        
        ticketData.setSignature(signature);
        
        System.out.println("票证签名完成 - 票证ID: " + ticket.getTicketId() + ", 签名长度: " + signature.length());
        System.out.println("完整二维码数据: " + ticketData.toQRString());

        return ticketData;
    }

    @Override
    public String generateSignedQRData(Ticket ticket) {
        System.out.println("开始生成二维码数据 - 票证ID: " + ticket.getTicketId());
        DigitalTicketData ticketData = generateSignedTicketData(ticket);
        String qrData = ticketData.toQRString();
        System.out.println("二维码数据生成完成 - 票证ID: " + ticket.getTicketId() + ", 数据长度: " + qrData.length());
        return qrData;
    }

    @Override
    public String getPublicKeyBase64() {
        return this.publicKeyBase64;
    }

    /**
     * 压缩签名数据以适应数据库限制
     * 使用SHA-256哈希算法处理原始签名，确保长度不超过数据库字段长度
     */
    private String compressSignature(String originalSignature) {
        try {
            System.out.println("压缩签名数据 - 原始长度: " + originalSignature.length());
            
            if (originalSignature.length() <= MAX_SIGNATURE_LENGTH) {
                System.out.println("签名无需压缩，原始长度未超过限制");
                return originalSignature;
            }
            
            // 使用SHA-256哈希处理原始签名，生成定长输出
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalSignature.getBytes());
            String hashedSignature = java.util.Base64.getEncoder().encodeToString(hashBytes);
            
            // SHA-256的Base64编码输出应该约为44个字符，远低于varchar(255)限制
            System.out.println("签名压缩完成 - 哈希后长度: " + hashedSignature.length() + 
                             ", 哈希值: " + hashedSignature.substring(0, Math.min(20, hashedSignature.length())) + "...");
            return hashedSignature;
        } catch (Exception e) {
            System.err.println("压缩签名数据失败: " + e.getMessage());
            e.printStackTrace();
            // 出错时截取最大长度
            return originalSignature.substring(0, Math.min(originalSignature.length(), MAX_SIGNATURE_LENGTH));
        }
    }
    
    @Override
    public boolean verifyTicketSignature(String qrData) {
        System.out.println("开始验证票证签名 - 数据长度: " + qrData.length());
        try {
            DigitalTicketData ticketData = DigitalTicketData.fromQRString(qrData);
            System.out.println("解析二维码数据成功 - 票证ID: " + ticketData.getTicketId() + 
                             ", 票证号: " + ticketData.getTicketNumber() + 
                             ", 乘客: " + ticketData.getPassengerName());
            
            String dataToVerify = ticketData.getDataToSign();
            System.out.println("待验证数据: " + dataToVerify);
            System.out.println("签名数据长度: " + ticketData.getSignature().length());
            
            // 原始签名已被压缩，我们需要在验证时采用同样的方式处理
            String originalSignature = cryptoUtil.sign(dataToVerify.getBytes(), privateKey);
            String compressedOriginalSignature = compressSignature(originalSignature);
            
            // 比较压缩后的签名
            boolean result = compressedOriginalSignature.equals(ticketData.getSignature());
            System.out.println("验证结果: " + (result ? "成功" : "失败") + " - 票证ID: " + ticketData.getTicketId());
            
            return result;
        } catch (Exception e) {
            System.err.println("验证票证签名时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
