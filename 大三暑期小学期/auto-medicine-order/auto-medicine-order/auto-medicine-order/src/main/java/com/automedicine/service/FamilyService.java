package com.automedicine.service;

import com.automedicine.dto.request.FamilyBindRequest;
import com.automedicine.dto.response.FamilyMemberResponse;
import com.automedicine.dto.response.UserResponse;
import com.automedicine.entity.FamilyRelation;
import com.automedicine.entity.User;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.FamilyRelationRepository;
import com.automedicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 亲友关系服务
 * <p>
 * 通过 familyId 搜索用户 → 发起绑定 → 接受/拒绝 → 查询亲友列表。
 * WebSocket 推送通知在阶段十四补充。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final UserRepository userRepository;
    private final FamilyRelationRepository familyRelationRepository;
    private final NotificationService notificationService;

    /**
     * 通过 familyId 搜索用户（脱敏，不返回密码）
     */
    @Transactional(readOnly = true)
    public UserResponse searchByFamilyId(String familyId) {
        User user = userRepository.findByFamilyId(familyId)
                .orElseThrow(() -> new BusinessException("未找到该亲友标识对应的用户", 404));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(maskPhone(user.getPhone()))
                .role(user.getRole())
                .familyId(user.getFamilyId())
                .build();
    }

    /**
     * 发起亲友绑定请求
     */
    @Transactional
    public void sendBindRequest(String username, FamilyBindRequest request) {
        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        // 不能绑定自己
        if (requester.getFamilyId() != null
                && requester.getFamilyId().equals(request.getTargetFamilyId())) {
            throw new BusinessException("不能与自己绑定", 400);
        }

        User target = userRepository.findByFamilyId(request.getTargetFamilyId())
                .orElseThrow(() -> new BusinessException("未找到该亲友标识对应的用户", 404));

        // 检查是否已存在关系
        if (familyRelationRepository.existsRelationBetween(requester.getId(), target.getId())) {
            throw new BusinessException("您与该用户已存在亲友关系", 400);
        }

        // 创建 PENDING 状态的绑定记录
        FamilyRelation relation = new FamilyRelation();
        relation.setRequester(requester);
        relation.setTarget(target);
        relation.setRelationship(request.getRelationship());
        relation.setStatus(FamilyRelation.RelationStatus.PENDING);
        familyRelationRepository.save(relation);

        // 给对方发送通知
        String requesterName = requester.getRealName() != null
                ? requester.getRealName() : requester.getUsername();
        notificationService.createNotification(
                target.getId(),
                "亲友绑定请求",
                requesterName + " 请求与您绑定为" + request.getRelationship() + "关系",
                com.automedicine.enums.NotificationType.FAMILY,
                null,
                relation.getId()
        );
    }

    /**
     * 接受绑定请求
     */
    @Transactional
    public void acceptBind(Long relationId, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        FamilyRelation relation = familyRelationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException("绑定记录不存在", 404));

        // 只有目标用户可以接受
        if (!relation.getTarget().getId().equals(currentUser.getId())) {
            throw new BusinessException("您无权操作此绑定请求", 403);
        }
        if (relation.getStatus() != FamilyRelation.RelationStatus.PENDING) {
            throw new BusinessException("该绑定请求已被处理", 400);
        }

        relation.setStatus(FamilyRelation.RelationStatus.ACCEPTED);
        familyRelationRepository.save(relation);

        // 通知发起方绑定成功
        String targetName = currentUser.getRealName() != null
                ? currentUser.getRealName() : currentUser.getUsername();
        notificationService.createNotification(
                relation.getRequester().getId(),
                "亲友绑定成功",
                targetName + " 已接受您的绑定请求",
                com.automedicine.enums.NotificationType.FAMILY,
                null
        );
    }

    /**
     * 拒绝绑定请求
     */
    @Transactional
    public void rejectBind(Long relationId, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        FamilyRelation relation = familyRelationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException("绑定记录不存在", 404));

        if (!relation.getTarget().getId().equals(currentUser.getId())) {
            throw new BusinessException("您无权操作此绑定请求", 403);
        }
        if (relation.getStatus() != FamilyRelation.RelationStatus.PENDING) {
            throw new BusinessException("该绑定请求已被处理", 400);
        }

        relation.setStatus(FamilyRelation.RelationStatus.REJECTED);
        familyRelationRepository.save(relation);

        // 通知发起方绑定被拒绝
        String targetName = currentUser.getRealName() != null
                ? currentUser.getRealName() : currentUser.getUsername();
        notificationService.createNotification(
                relation.getRequester().getId(),
                "亲友绑定被拒绝",
                targetName + " 拒绝了您的绑定请求",
                com.automedicine.enums.NotificationType.FAMILY,
                null
        );
    }

    /**
     * 查询我的亲友列表（仅已接受的）
     */
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getFamilyMembers(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        List<FamilyRelation> relations = familyRelationRepository
                .findAcceptedRelationsByUserId(user.getId());

        return relations.stream().map(r -> {
            // 判断当前用户是发起方还是接收方，找到对方用户
            boolean isRequester = r.getRequester().getId().equals(user.getId());
            User otherUser = isRequester ? r.getTarget() : r.getRequester();

            return FamilyMemberResponse.builder()
                    .userId(otherUser.getId())
                    .username(otherUser.getUsername())
                    .realName(otherUser.getRealName())
                    .phone(otherUser.getPhone())
                    .familyId(otherUser.getFamilyId())
                    .relationship(r.getRelationship())
                    .relationRole(isRequester ? "发起方" : "接收方")
                    .build();
        }).toList();
    }

    /**
     * 手机号脱敏：138****0001
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
