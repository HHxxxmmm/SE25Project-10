package com.example.techprototype.Service;

import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.DTO.DigitalTicketData;
import org.springframework.stereotype.Service;

/**
 * 数字票证服务接口
 */
public interface DigitalTicketService {

    /**
     * 为车票生成签名数据
     *
     * @param ticket 车票对象
     * @return 签名后的票证数据
     */
    DigitalTicketData generateSignedTicketData(Ticket ticket);

    /**
     * 为车票生成带签名的二维码数据
     *
     * @param ticket 车票对象
     * @return 二维码数据字符串
     */
    String generateSignedQRData(Ticket ticket);

    /**
     * 获取签名用的公钥（Base64编码）
     *
     * @return 公钥字符串
     */
    String getPublicKeyBase64();

    /**
     * 验证票证签名
     *
     * @param qrData 二维码数据
     * @return 验证结果
     */
    boolean verifyTicketSignature(String qrData);
}
