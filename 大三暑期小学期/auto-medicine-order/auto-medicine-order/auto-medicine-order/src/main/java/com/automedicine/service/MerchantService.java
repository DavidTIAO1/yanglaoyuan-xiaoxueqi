package com.automedicine.service;

import com.automedicine.dto.response.MedicineResponse;
import com.automedicine.dto.response.MerchantResponse;
import com.automedicine.entity.Medicine;
import com.automedicine.entity.Merchant;
import com.automedicine.entity.User;
import com.automedicine.enums.UserRole;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.MedicineRepository;
import com.automedicine.repository.MerchantRepository;
import com.automedicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商家服务
 * <p>
 * 提供商家资料的获取、更新（首次更新时自动创建 Merchant 记录）、
 * 附近商家查询和商家药品列表查询。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;

    /**
     * 获取当前商家的资料
     */
    public MerchantResponse getProfile(String username) {
        Merchant merchant = findMerchantByUsername(username);
        return toMerchantResponse(merchant);
    }

    /**
     * 更新商家资料
     * <p>
     * 若商家记录不存在（首次更新），则自动创建并关联 User，状态默认为 PENDING。
     * </p>
     */
    @Transactional
    public MerchantResponse updateProfile(String username, String shopName, String licenseNo,
                                           String address, BigDecimal latitude, BigDecimal longitude,
                                           String phone, Integer deliveryRadius) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        if (user.getRole() != UserRole.MERCHANT) {
            throw new BusinessException("仅商家角色可操作", 403);
        }

        Merchant merchant = merchantRepository.findByUserId(user.getId()).orElse(null);

        if (merchant == null) {
            // 首次更新，创建商家记录
            merchant = new Merchant();
            merchant.setUser(user);
        }

        if (shopName != null) merchant.setShopName(shopName);
        if (licenseNo != null) merchant.setLicenseNo(licenseNo);
        if (address != null) merchant.setAddress(address);
        if (latitude != null) merchant.setLatitude(latitude);
        if (longitude != null) merchant.setLongitude(longitude);
        if (phone != null) merchant.setPhone(phone);
        if (deliveryRadius != null) merchant.setDeliveryRadius(deliveryRadius);

        merchantRepository.save(merchant);
        return toMerchantResponse(merchant);
    }

    /**
     * 查询附近商家
     * <p>
     * 使用 Haversine 公式按距离升序返回 APPROVED 状态且在指定半径内的商家。
     * </p>
     *
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param radiusKm  搜索半径（公里），默认 5
     */
    public List<MerchantResponse> findNearby(BigDecimal latitude, BigDecimal longitude, int radiusKm) {
        return merchantRepository.findNearbyMerchants(latitude, longitude, radiusKm)
                .stream()
                .map(this::toMerchantResponse)
                .toList();
    }

    /**
     * 获取指定商家的药品列表（仅上架状态）
     */
    public List<MedicineResponse> getMedicines(Long merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new BusinessException("商家不存在", 404);
        }
        return medicineRepository.findByMerchantIdAndStatusOrderByIdDesc(merchantId, 1)
                .stream()
                .map(this::toMedicineResponse)
                .toList();
    }

    private Merchant findMerchantByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));
        return merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("商家资料不存在，请先完善店铺信息", 404));
    }

    private MerchantResponse toMerchantResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .shopName(merchant.getShopName())
                .licenseNo(merchant.getLicenseNo())
                .address(merchant.getAddress())
                .latitude(merchant.getLatitude())
                .longitude(merchant.getLongitude())
                .phone(merchant.getPhone())
                .status(merchant.getStatus())
                .deliveryRadius(merchant.getDeliveryRadius())
                .build();
    }

    private MedicineResponse toMedicineResponse(Medicine m) {
        return MedicineResponse.builder()
                .id(m.getId())
                .merchantId(m.getMerchant().getId())
                .name(m.getName())
                .genericName(m.getGenericName())
                .category(m.getCategory())
                .price(m.getPrice())
                .stock(m.getStock())
                .description(m.getDescription())
                .requiresPrescription(m.getRequiresPrescription())
                .status(m.getStatus())
                .build();
    }
}
