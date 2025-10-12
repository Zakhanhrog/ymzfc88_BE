package com.xsecret.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request để tạo/cập nhật kết quả xổ số
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryResultRequest {
    
    /**
     * Vùng miền: mienBac, mienTrungNam
     */
    private String region;
    
    /**
     * Tỉnh (chỉ cho Miền Trung Nam)
     * Các giá trị: gialai, binhduong, ninhthuan, travinh, vinhlong
     */
    private String province;
    
    /**
     * Ngày quay thưởng (format: yyyy-MM-dd)
     */
    private String drawDate;
    
    /**
     * Kết quả các giải (JSON format)
     * 
     * Miền Bắc (7 giải):
     * {
     *   "dac-biet": "00943",
     *   "giai-nhat": "43213",
     *   "giai-nhi": ["66146", "15901"],
     *   "giai-ba": ["22906", "04955", "93893", "32538", "25660", "85773"],
     *   "giai-tu": ["8964", "0803", "4867", "2405"],
     *   "giai-nam": ["9122", "6281", "8813", "6672", "8101", "7293"],
     *   "giai-sau": ["803", "301", "325"],
     *   "giai-bay": ["84", "09", "69", "79"]
     * }
     * 
     * Miền Trung Nam (8 giải):
     * {
     *   "dac-biet": "042293",
     *   "giai-nhat": "02518",
     *   "giai-nhi": ["49226"],
     *   "giai-ba": ["03856", "04216"],
     *   "giai-tu": ["00810", "02321", "00681", "51728", "24507", "58068", "96136"],
     *   "giai-nam": ["8877"],
     *   "giai-sau": ["5934", "7442", "3430"],
     *   "giai-bay": ["884"],
     *   "giai-tam": ["40"]
     * }
     */
    private String results;
    
    /**
     * Trạng thái: DRAFT, PUBLISHED
     */
    private String status;
}

