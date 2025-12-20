package com.xsecret.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class OkdpayService {

    // ====== CONFIG - Dùng trực tiếp trong code như mẫu ======
    private static final String PAY_GATEWAY = "https://shapi.okdpay888.top/v1/dsapi/add2";
    private static final String MCH_ID = "9182";
    private static final String API_KEY = "iFilLS5aURGLl4der7krYAZ3LqfwWKJV6O0wDux3jbX50RdH83btRtik31KYzoje";
    private static final String NOTIFY_URL = "https://api.tathiet168.com/api/callbackbank";
    private static final String RETURN_URL = "https://tathiet168.com";
    private static final String OKDPAY_CALLBACK_IP = "45.58.184.162";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Format datetime: YYYY-MM-DD HH:mm:ss (y hệt mẫu)
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Format money: 2 chữ số thập phân (y hệt mẫu)
     */
    private String toMoney2(BigDecimal amount) {
        return String.format("%.2f", amount.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
    }

    /**
     * Tạo chữ ký MD5 theo thuật toán OKDPAY (y hệt mẫu signParams)
     */
    private String signParams(Map<String, String> paramsForSign, String apiKey) {
        // Lọc bỏ undefined/null/empty và sort (y hệt mẫu)
        List<String> sortedKeys = new ArrayList<>();
        for (Map.Entry<String, String> entry : paramsForSign.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                sortedKeys.add(entry.getKey());
            }
        }
        Collections.sort(sortedKeys);

        // Ghép key=value&key=value (y hệt mẫu)
        StringBuilder kv = new StringBuilder();
        for (int i = 0; i < sortedKeys.size(); i++) {
            if (i > 0) {
                kv.append("&");
            }
            kv.append(sortedKeys.get(i))
              .append("=")
              .append(paramsForSign.get(sortedKeys.get(i)));
        }

        // Thêm &key=API_KEY (y hệt mẫu)
        String stringSignTemp = kv.toString() + "&key=" + apiKey;

        // MD5 và uppercase (y hệt mẫu)
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(stringSignTemp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            log.error("Error generating signature: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signature: " + e.getMessage());
        }
    }

    /**
     * Tạo đơn thanh toán tự động (y hệt mẫu createTransaction)
     */
    public OkdpayCreateOrderResponse createOrder(
            String channelCode,
            BigDecimal amount) {

        try {
            // out_trade_no: Date.now() như mẫu (line 84)
            String out_trade_no = String.valueOf(System.currentTimeMillis());
            String applydate = formatDateTime(LocalDateTime.now());
            String money = toMoney2(amount);

            // Base params tham gia ký (y hệt mẫu line 88-96)
            // Dùng NOTIFY_URL constant như mẫu (line 92)
            Map<String, String> baseParams = new HashMap<>();
            baseParams.put("mchid", MCH_ID);
            baseParams.put("out_trade_no", out_trade_no);
            baseParams.put("money", money);
            baseParams.put("notifyurl", NOTIFY_URL); // Dùng constant như mẫu
            baseParams.put("code", channelCode);
            baseParams.put("applydate", applydate);

            // Optional params KHÔNG tham gia ký (y hệt mẫu line 99-104)
            // Dùng RETURN_URL constant như mẫu (line 100)
            Map<String, String> optionalParams = new HashMap<>();
            optionalParams.put("returnurl", RETURN_URL); // Dùng constant như mẫu
            optionalParams.put("productname", "Nạp " + money);
            optionalParams.put("attach", "");
            optionalParams.put("submitname", ""); // Để trống như mẫu

            // Tạo signature (y hệt mẫu line 106)
            String sign = signParams(baseParams, API_KEY);

            // Payload đầy đủ (y hệt mẫu line 108-112)
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            baseParams.forEach(formData::add);
            optionalParams.forEach(formData::add);
            formData.add("sign", sign);

            // Gọi API (y hệt mẫu line 117-124)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            log.info("Calling OKDPAY: {}", PAY_GATEWAY);
            log.info("Request - Channel: {}, Amount: {}, OutTradeNo: {}", channelCode, money, out_trade_no);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    PAY_GATEWAY,
                    request,
                    String.class
            );

            // LOG RESPONSE TRƯỚC KHI PARSE (y hệt mẫu line 131)
            log.info("OKDPAY response body: {}", response.getBody());

            // Parse response
            JsonNode respData = objectMapper.readTree(response.getBody());

            // Check status như mẫu (line 132-134)
            String status = respData.path("status").asText();
            if (!"success".equalsIgnoreCase(status)) {
                String msg = respData.path("msg").asText();
                log.error("OKDPAY returned error - Status: {}, Msg: {}", status, msg);
                OkdpayCreateOrderResponse errorResponse = new OkdpayCreateOrderResponse();
                errorResponse.setStatus(status);
                errorResponse.setMsg(msg);
                return errorResponse;
            }

            // Parse data như mẫu (line 135-139)
            OkdpayCreateOrderResponse result = new OkdpayCreateOrderResponse();
            result.setStatus(status);
            result.setMsg(respData.path("msg").asText());
            result.setOrderNo(respData.path("order_no").asText());
            result.setOutTradeNo(respData.path("out_trade_no").asText());
            
            // Lấy pay_url và html như mẫu (dùng || '' fallback - line 138-139)
            String pay_url = respData.path("pay_url").isMissingNode() ? "" : respData.path("pay_url").asText();
            String html = respData.path("html").isMissingNode() ? "" : respData.path("html").asText();
            
            result.setPayUrl(pay_url);
            result.setHtml(html);
            result.setSign(respData.path("sign").asText());

            log.info("OKDPAY success - OrderNo: {}, PayUrl: {}, Html: {}", 
                    result.getOrderNo(), 
                    pay_url.isEmpty() ? "EMPTY" : "EXISTS",
                    html.isEmpty() ? "EMPTY" : "EXISTS");

            return result;

        } catch (Exception e) {
            log.error("Error creating OKDPAY order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create OKDPAY order: " + e.getMessage());
        }
    }

    /**
     * Xác thực chữ ký từ callback
     */
    public boolean verifyCallbackSignature(Map<String, String> callbackParams, String apiKey) {
        try {
            String receivedSign = callbackParams.get("sign");
            if (receivedSign == null || receivedSign.isEmpty()) {
                return false;
            }

            // Chỉ ký các tham số tham gia ký: mchid,out_trade_no,amount,transaction_id,refCode,refMsg
            Set<String> signKeys = Set.of("mchid", "out_trade_no", "amount", "transaction_id", "refCode", "refMsg");

            Map<String, String> signParams = new HashMap<>();
            for (Map.Entry<String, String> entry : callbackParams.entrySet()) {
                if (!entry.getKey().equals("sign") && signKeys.contains(entry.getKey())) {
                    signParams.put(entry.getKey(), entry.getValue());
                }
            }

            String calculatedSign = signParams(signParams, apiKey);
            boolean isValid = receivedSign.equalsIgnoreCase(calculatedSign);
            
            if (!isValid) {
                log.warn("Signature mismatch. Received: {}, Calculated: {}", receivedSign, calculatedSign);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying callback signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kiểm tra IP callback có hợp lệ không
     */
    public boolean isValidCallbackIp(String ip) {
        return OKDPAY_CALLBACK_IP.equals(ip);
    }

    /**
     * Query order status từ OKDPAY API (theo tài liệu line 151-186)
     * Dùng để check lại transaction status khi callback không đến được (ví dụ: chạy local)
     */
    public OkdpayQueryOrderResponse queryOrder(String outTradeNo) {
        try {
            // Base params tham gia ký (theo tài liệu line 168-169)
            Map<String, String> baseParams = new HashMap<>();
            baseParams.put("mchid", MCH_ID);
            baseParams.put("out_trade_no", outTradeNo);

            // Tạo signature
            String sign = signParams(baseParams, API_KEY);

            // Payload
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            baseParams.forEach(formData::add);
            formData.add("sign", sign);

            // Gọi API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            String queryUrl = "https://shapi.okdpay888.top/v1/dsapi/query_order";
            log.info("Querying OKDPAY order status: {}", queryUrl);
            log.info("Request - OutTradeNo: {}", outTradeNo);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    queryUrl,
                    request,
                    String.class
            );

            log.info("OKDPAY query response body: {}", response.getBody());

            // Parse response
            JsonNode respData = objectMapper.readTree(response.getBody());

            // Check status
            String status = respData.path("status").asText();
            if (!"success".equalsIgnoreCase(status)) {
                String msg = respData.path("msg").asText();
                log.error("OKDPAY query returned error - Status: {}, Msg: {}", status, msg);
                OkdpayQueryOrderResponse errorResponse = new OkdpayQueryOrderResponse();
                errorResponse.setStatus(status);
                errorResponse.setMsg(msg);
                return errorResponse;
            }

            // Parse data
            OkdpayQueryOrderResponse result = new OkdpayQueryOrderResponse();
            result.setStatus(status);
            result.setMsg(respData.path("msg").asText());
            result.setMchid(respData.path("mchid").asText());
            result.setOutTradeNo(respData.path("out_trade_no").asText());
            result.setAmount(respData.path("amount").asText());
            result.setTransactionId(respData.path("transaction_id").asText());
            result.setRefCode(respData.path("refCode").asText());
            result.setRefMsg(respData.path("refMsg").asText());
            result.setSuccessTime(respData.path("success_time").asText());
            result.setAttach(respData.path("attach").asText());
            result.setSign(respData.path("sign").asText());

            log.info("OKDPAY query success - OutTradeNo: {}, RefCode: {}, Amount: {}", 
                    result.getOutTradeNo(), result.getRefCode(), result.getAmount());

            return result;

        } catch (Exception e) {
            log.error("Error querying OKDPAY order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query OKDPAY order: " + e.getMessage());
        }
    }

    // Query Order Response DTO
    public static class OkdpayQueryOrderResponse {
        private String status;
        private String msg;
        private String mchid;
        private String outTradeNo;
        private String amount;
        private String transactionId;
        private String refCode;
        private String refMsg;
        private String successTime;
        private String attach;
        private String sign;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public String getMchid() { return mchid; }
        public void setMchid(String mchid) { this.mchid = mchid; }
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getRefCode() { return refCode; }
        public void setRefCode(String refCode) { this.refCode = refCode; }
        public String getRefMsg() { return refMsg; }
        public void setRefMsg(String refMsg) { this.refMsg = refMsg; }
        public String getSuccessTime() { return successTime; }
        public void setSuccessTime(String successTime) { this.successTime = successTime; }
        public String getAttach() { return attach; }
        public void setAttach(String attach) { this.attach = attach; }
        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }
    }

    // Response DTO
    public static class OkdpayCreateOrderResponse {
        private String status;
        private String msg;
        private String orderNo;
        private String outTradeNo;
        private String html;
        private String payUrl;
        private String sign;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }
        public String getPayUrl() { return payUrl; }
        public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }
    }
}
