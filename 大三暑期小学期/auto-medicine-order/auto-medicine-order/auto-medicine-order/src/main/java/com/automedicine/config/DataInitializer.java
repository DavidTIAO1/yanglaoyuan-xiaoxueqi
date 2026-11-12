package com.automedicine.config;

import com.automedicine.entity.Merchant;
import com.automedicine.entity.User;
import com.automedicine.enums.UserRole;
import com.automedicine.repository.MerchantRepository;
import com.automedicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据初始化 — 应用启动时自动补全缺失的 Merchant 记录
 * <p>
 * 解决早期注册的商家用户没有 merchants 表记录导致 API 404 的问题。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;

    @Override
    public void run(String... args) {
        List<User> merchants = userRepository.findByRole(UserRole.MERCHANT);
        int created = 0;
        for (User user : merchants) {
            if (merchantRepository.findByUserId(user.getId()).isEmpty()) {
                Merchant merchant = new Merchant();
                merchant.setUser(user);
                merchant.setShopName(user.getRealName() != null
                        ? user.getRealName() + "的店铺" : "新店铺");
                merchant.setStatus(Merchant.MerchantStatus.APPROVED);
                merchantRepository.save(merchant);
                created++;
                log.info("为已有商家 {} 自动创建了 Merchant 记录", user.getUsername());
            }
        }
        if (created > 0) {
            log.info("数据初始化完成：补全了 {} 个缺失的 Merchant 记录", created);
        }
    }
}
