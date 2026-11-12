package com.automedicine.service;

import com.automedicine.dto.request.MedicineRequest;
import com.automedicine.dto.response.MedicineResponse;
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

import java.util.List;

/**
 * 药品服务
 * <p>
 * 提供药品的增删改查操作，仅限 MERCHANT 角色调用。
 * 所有操作均基于当前登录用户关联的商家。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    /**
     * 新增药品
     */
    @Transactional
    public MedicineResponse addMedicine(String username, MedicineRequest request) {
        Merchant merchant = findMerchantByUsername(username);

        Medicine medicine = new Medicine();
        medicine.setMerchant(merchant);
        medicine.setName(request.getName());
        medicine.setGenericName(request.getGenericName());
        medicine.setCategory(request.getCategory());
        medicine.setPrice(request.getPrice());
        medicine.setStock(request.getStock() != null ? request.getStock() : 0);
        medicine.setDescription(request.getDescription());
        medicine.setRequiresPrescription(request.getRequiresPrescription() != null ? request.getRequiresPrescription() : false);
        medicine.setStatus(1);

        medicineRepository.save(medicine);
        return toMedicineResponse(medicine);
    }

    /**
     * 查询当前商家的药品列表（仅上架状态）
     */
    public List<MedicineResponse> getMyMedicines(String username) {
        Merchant merchant = findMerchantByUsername(username);
        return medicineRepository.findByMerchantIdAndStatusOrderByIdDesc(merchant.getId(), 1)
                .stream()
                .map(this::toMedicineResponse)
                .toList();
    }

    /**
     * 修改药品信息
     */
    @Transactional
    public MedicineResponse updateMedicine(String username, Long medicineId, MedicineRequest request) {
        Merchant merchant = findMerchantByUsername(username);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new BusinessException("药品不存在", 404));

        if (!medicine.getMerchant().getId().equals(merchant.getId())) {
            throw new BusinessException("无权修改其他商家的药品", 403);
        }

        if (request.getName() != null) medicine.setName(request.getName());
        if (request.getGenericName() != null) medicine.setGenericName(request.getGenericName());
        if (request.getCategory() != null) medicine.setCategory(request.getCategory());
        if (request.getPrice() != null) medicine.setPrice(request.getPrice());
        if (request.getStock() != null) medicine.setStock(request.getStock());
        if (request.getDescription() != null) medicine.setDescription(request.getDescription());
        if (request.getRequiresPrescription() != null) medicine.setRequiresPrescription(request.getRequiresPrescription());

        medicineRepository.save(medicine);
        return toMedicineResponse(medicine);
    }

    /**
     * 下架药品（逻辑删除，status 置为 0）
     */
    @Transactional
    public void deleteMedicine(String username, Long medicineId) {
        Merchant merchant = findMerchantByUsername(username);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new BusinessException("药品不存在", 404));

        if (!medicine.getMerchant().getId().equals(merchant.getId())) {
            throw new BusinessException("无权修改其他商家的药品", 403);
        }

        medicine.setStatus(0);
        medicineRepository.save(medicine);
    }

    private Merchant findMerchantByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));
        if (user.getRole() != UserRole.MERCHANT) {
            throw new BusinessException("仅商家可操作药品", 403);
        }
        return merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("商家资料不存在，请先完善店铺信息", 404));
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
