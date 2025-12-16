package com.xsecret.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OkdpayService {

    private static final String OKDPAY_BASE_URL = "https://shapi.okdpay888.top";
    private static final String OKDPAY_CALLBACK_IP = "45.58.184.162";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tạo chữ ký MD5 theo thuật toán OKDPAY
     */
    public String generateSignature(Map<String, String> params, String apiKey) {
        try {
            return generateSignature(params, apiKey, null);

        } catch (Exception e) {
            log.error("Error generating signature: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signature: " + e.getMessage());
        }
    }

    /**
     * Tạo chữ ký MD5 theo thuật toán OKDPAY, cho phép giới hạn danh sách key tham gia ký
     */
    public String generateSignature(Map<String, String> params, String apiKey, Set<String> signKeys) {
        try {
            // Bước 1: Loại bỏ các tham số rỗng, bỏ sign, và chỉ lấy tham số thuộc signKeys (nếu có)
            Map<String, String> signParams = params.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty())
                    .filter(entry -> !entry.getKey().equals("sign"))
                    .filter(entry -> signKeys == null || signKeys.contains(entry.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            // Bước 2: Sắp xếp theo ASCII tăng dần (từ điển)
            List<String> sortedKeys = new ArrayList<>(signParams.keySet());
            Collections.sort(sortedKeys);

            // Bước 3: Ghép chuỗi theo định dạng key1=value1&key2=value2
            StringBuilder signString = new StringBuilder();
            for (int i = 0; i < sortedKeys.size(); i++) {
                if (i > 0) {
                    signString.append("&");
                }
                signString.append(sortedKeys.get(i))
                        .append("=")
                        .append(signParams.get(sortedKeys.get(i)));
            }

            // Bước 4: Ghép thêm &key=API_KEY vào cuối
            signString.append("&key=").append(apiKey);

            // Bước 5: Thực hiện MD5 và chuyển sang chữ IN HOA
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(signString.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String signature = hexString.toString().toUpperCase();
            log.debug("Signature generated: {}", signature);
            log.debug("Sign string: {}", signString.toString());
            return signature;

        } catch (Exception e) {
            log.error("Error generating signature: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signature: " + e.getMessage());
        }
    }

    /**
     * Tạo đơn thanh toán tự động
     */
    public OkdpayCreateOrderResponse createOrder(
            String merchantId,
            String apiKey,
            String channelCode,
            String outTradeNo,
            BigDecimal amount,
            String notifyUrl,
            String returnUrl) {

        try {
            // Các key tham gia ký theo tài liệu OKDPAY add2: mchid,out_trade_no,money,notifyurl,code,applydate
            final Set<String> SIGN_KEYS = Set.of("mchid", "out_trade_no", "money", "notifyurl", "code", "applydate");

            // Chuẩn bị tham số
            Map<String, String> params = new HashMap<>();
            params.put("mchid", merchantId);
            params.put("out_trade_no", outTradeNo);
            params.put("money", amount.setScale(2, java.math.RoundingMode.HALF_UP).toString());
            params.put("notifyurl", notifyUrl);
            params.put("code", channelCode); // Mã kênh từ PaymentMethod
            params.put("applydate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            if (returnUrl != null && !returnUrl.trim().isEmpty()) {
                params.put("returnurl", returnUrl);
            }
            
            params.put("productname", "Nạp tiền vào tài khoản");
            params.put("attach", "deposit_" + outTradeNo);

            // Tạo chữ ký
            String sign = generateSignature(params, apiKey, SIGN_KEYS);
            params.put("sign", sign);

            // Gửi request dạng FORM
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            params.forEach(formData::add);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            log.info("Calling OKDPAY create order API: {}", OKDPAY_BASE_URL + "/v1/dsapi/add2");
            log.debug("Request params: {}", params);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    OKDPAY_BASE_URL + "/v1/dsapi/add2",
                    request,
                    String.class
            );

            log.info("OKDPAY response status: {}", response.getStatusCode());
            log.info("OKDPAY response body: {}", response.getBody());

            // Parse response JSON
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            OkdpayCreateOrderResponse result = new OkdpayCreateOrderResponse();
            result.setStatus(jsonNode.path("status").asText());
            result.setMsg(jsonNode.path("msg").asText());
            result.setOrderNo(jsonNode.path("order_no").asText());
            result.setOutTradeNo(jsonNode.path("out_trade_no").asText());
            result.setHtml(jsonNode.path("html").asText());
            result.setPayUrl(jsonNode.path("pay_url").asText());
            result.setSign(jsonNode.path("sign").asText());
            
            // Log chi tiết response để debug
            log.info("Parsed OKDPAY response - Status: {}, Msg: {}, OrderNo: {}, PayUrl: {}", 
                    result.getStatus(), result.getMsg(), result.getOrderNo(), result.getPayUrl());

            // Verify signature từ response
            if (!verifyResponseSignature(jsonNode, apiKey)) {
                log.warn("Response signature verification failed for order: {}", outTradeNo);
            }

            return result;

        } catch (Exception e) {
            log.error("Error creating OKDPAY order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create OKDPAY order: " + e.getMessage());
        }
    }

    /**
     * Truy vấn trạng thái đơn hàng
     */
    public OkdpayQueryOrderResponse queryOrder(String merchantId, String apiKey, String outTradeNo) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("mchid", merchantId);
            params.put("out_trade_no", outTradeNo);

            String sign = generateSignature(params, apiKey);
            params.put("sign", sign);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            params.forEach(formData::add);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    OKDPAY_BASE_URL + "/v1/dsapi/query_order",
                    request,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            OkdpayQueryOrderResponse result = new OkdpayQueryOrderResponse();
            result.setStatus(jsonNode.path("status").asText());
            result.setMsg(jsonNode.path("msg").asText());
            result.setMchid(jsonNode.path("mchid").asText());
            result.setOutTradeNo(jsonNode.path("out_trade_no").asText());
            result.setAmount(jsonNode.path("amount").asText());
            result.setTransactionId(jsonNode.path("transaction_id").asText());
            result.setRefCode(jsonNode.path("refCode").asText());
            result.setRefMsg(jsonNode.path("refMsg").asText());
            result.setSuccessTime(jsonNode.path("success_time").asText());
            result.setAttach(jsonNode.path("attach").asText());

            return result;

        } catch (Exception e) {
            log.error("Error querying OKDPAY order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query OKDPAY order: " + e.getMessage());
        }
    }

    /**
     * Truy vấn số dư
     */
    public OkdpayBalanceResponse queryBalance(String merchantId, String apiKey) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("mchid", merchantId);

            String sign = generateSignature(params, apiKey);
            params.put("sign", sign);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            params.forEach(formData::add);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    OKDPAY_BASE_URL + "/v1/dsapi/query_balance",
                    request,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            OkdpayBalanceResponse result = new OkdpayBalanceResponse();
            result.setStatus(jsonNode.path("status").asText());
            result.setMsg(jsonNode.path("msg").asText());
            result.setBalance(jsonNode.path("balance").asText());
            result.setFreezeBalance(jsonNode.path("freeze_balance").asText());

            return result;

        } catch (Exception e) {
            log.error("Error querying OKDPAY balance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query OKDPAY balance: " + e.getMessage());
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

            // Chỉ ký các tham số tham gia ký theo tài liệu callback: mchid,out_trade_no,amount,transaction_id,refCode,refMsg
            Set<String> signKeys = Set.of("mchid", "out_trade_no", "amount", "transaction_id", "refCode", "refMsg");

            Map<String, String> signParams = new HashMap<>(callbackParams);
            signParams.remove("sign");

            String calculatedSign = generateSignature(signParams, apiKey, signKeys);
            
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
     * Xác thực chữ ký từ response
     */
    private boolean verifyResponseSignature(JsonNode jsonNode, String apiKey) {
        try {
            String receivedSign = jsonNode.path("sign").asText();
            if (receivedSign == null || receivedSign.isEmpty()) {
                return false;
            }

            // Chỉ ký các trường response có trong tài liệu add2
            Set<String> signKeys = Set.of("status", "msg", "order_no", "out_trade_no", "html", "pay_url");

            Map<String, String> params = new HashMap<>();
            jsonNode.fields().forEachRemaining(entry -> {
                if (!entry.getKey().equals("sign")) {
                    params.put(entry.getKey(), entry.getValue().asText());
                }
            });

            String calculatedSign = generateSignature(params, apiKey, signKeys);
            return receivedSign.equalsIgnoreCase(calculatedSign);
        } catch (Exception e) {
            log.error("Error verifying response signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kiểm tra IP callback có hợp lệ không
     */
    public boolean isValidCallbackIp(String ip) {
        return OKDPAY_CALLBACK_IP.equals(ip);
    }

    // Response DTOs
    public static class OkdpayCreateOrderResponse {
        private String status;
        private String msg;
        private String orderNo;
        private String outTradeNo;
        private String html;
        private String payUrl;
        private String sign;

        // Getters and Setters
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

        // Getters and Setters
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
    }

    public static class OkdpayBalanceResponse {
        private String status;
        private String msg;
        private String balance;
        private String freezeBalance;

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public String getBalance() { return balance; }
        public void setBalance(String balance) { this.balance = balance; }
        public String getFreezeBalance() { return freezeBalance; }
        public void setFreezeBalance(String freezeBalance) { this.freezeBalance = freezeBalance; }
    }
}
