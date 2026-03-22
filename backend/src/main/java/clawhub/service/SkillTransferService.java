package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillTransfer;
import clawhub.entity.User;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillTransferRepository;
import clawhub.repository.UserRepository;
import clawhub.websocket.SkillWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillTransferService {

    private final SkillTransferRepository transferRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final SkillWebSocketHandler webSocketHandler;

    private static final int TRANSFER_EXPIRY_DAYS = 7;
    private static final int MAX_PENDING_OUTGOING = 10;
    private static final int MAX_PENDING_INCOMING = 10;

    @Transactional(readOnly = true)
    public Page<SkillTransfer> listIncoming(User user, Pageable pageable) {
        return transferRepository.findPendingIncoming(user, Instant.now(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<SkillTransfer> listOutgoing(User user, Pageable pageable) {
        return transferRepository.findPendingOutgoing(user, Instant.now(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<SkillTransfer> listAllForUser(User user, Pageable pageable) {
        return transferRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<SkillTransfer> findById(UUID id) {
        return transferRepository.findById(id);
    }

    /**
     * 请求技能所有权转移
     */
    @Transactional
    public SkillTransfer requestTransfer(String skillSlug, String toUserHandle, String message, User fromUser) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillSlug));

        // 验证当前用户是技能所有者
        if (!skill.getOwner().getId().equals(fromUser.getId())) {
            throw new IllegalStateException("Only the skill owner can initiate a transfer");
        }

        // 不能转移给自己
        User toUser = userRepository.findByHandle(toUserHandle)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + toUserHandle));

        if (toUser.getId().equals(fromUser.getId())) {
            throw new IllegalArgumentException("Cannot transfer skill to yourself");
        }

        // 检查是否已有待处理的转移请求
        if (transferRepository.existsBySkillAndToUserAndStatus(
                skill, toUser, SkillTransfer.TransferStatus.PENDING)) {
            throw new IllegalStateException("A pending transfer request already exists for this user");
        }

        // 检查待处理请求数量限制
        long pendingOutgoing = transferRepository.countActiveBySkill(skill, Instant.now());
        if (pendingOutgoing >= MAX_PENDING_OUTGOING) {
            throw new IllegalStateException("Maximum pending outgoing transfers reached");
        }

        long pendingIncoming = transferRepository.findPendingIncoming(toUser, Instant.now(), Pageable.unpaged()).getTotalElements();
        if (pendingIncoming >= MAX_PENDING_INCOMING) {
            throw new IllegalStateException("Recipient has reached maximum pending incoming transfers");
        }

        SkillTransfer transfer = SkillTransfer.builder()
                .skill(skill)
                .fromUser(fromUser)
                .toUser(toUser)
                .requestMessage(message)
                .status(SkillTransfer.TransferStatus.PENDING)
                .expiresAt(Instant.now().plus(TRANSFER_EXPIRY_DAYS, ChronoUnit.DAYS))
                .build();

        SkillTransfer saved = transferRepository.save(transfer);
        log.info("Transfer requested: {} -> {} for skill {}", fromUser.getHandle(), toUserHandle, skillSlug);

        // 通知接收者
        webSocketHandler.notifyUser(toUser.getId(), "TRANSFER_REQUEST", Map.of(
                "transferId", saved.getId(),
                "skillSlug", skillSlug,
                "fromUser", fromUser.getHandle()
        ));

        return saved;
    }

    /**
     * 接受转移请求
     */
    @Transactional
    public SkillTransfer acceptTransfer(UUID transferId, String responseMessage, User currentUser) {
        SkillTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        if (!transfer.canAccept(currentUser)) {
            throw new IllegalStateException("Cannot accept this transfer");
        }

        Skill skill = transfer.getSkill();
        User newOwner = transfer.getToUser();
        User oldOwner = transfer.getFromUser();

        // 执行所有权转移
        skill.setOwner(newOwner);
        skillRepository.save(skill);

        // 更新转移状态
        transfer.setStatus(SkillTransfer.TransferStatus.ACCEPTED);
        transfer.setResponseMessage(responseMessage);
        transfer.setRespondedAt(Instant.now());
        SkillTransfer saved = transferRepository.save(transfer);

        log.info("Transfer accepted: {} now owns skill {}", newOwner.getHandle(), skill.getSlug());

        // 通知双方
        webSocketHandler.notifyUser(oldOwner.getId(), "TRANSFER_ACCEPTED", Map.of(
                "transferId", transferId,
                "skillSlug", skill.getSlug(),
                "toUser", newOwner.getHandle()
        ));

        return saved;
    }

    /**
     * 拒绝转移请求
     */
    @Transactional
    public SkillTransfer rejectTransfer(UUID transferId, String responseMessage, User currentUser) {
        SkillTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        if (!transfer.canReject(currentUser)) {
            throw new IllegalStateException("Cannot reject this transfer");
        }

        transfer.setStatus(SkillTransfer.TransferStatus.REJECTED);
        transfer.setResponseMessage(responseMessage);
        transfer.setRespondedAt(Instant.now());
        SkillTransfer saved = transferRepository.save(transfer);

        log.info("Transfer rejected: {} rejected skill {} from {}",
                currentUser.getHandle(), transfer.getSkill().getSlug(), transfer.getFromUser().getHandle());

        // 通知发起者
        webSocketHandler.notifyUser(transfer.getFromUser().getId(), "TRANSFER_REJECTED", Map.of(
                "transferId", transferId,
                "skillSlug", transfer.getSkill().getSlug(),
                "byUser", currentUser.getHandle()
        ));

        return saved;
    }

    /**
     * 取消转移请求
     */
    @Transactional
    public SkillTransfer cancelTransfer(UUID transferId, User currentUser) {
        SkillTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        if (!transfer.canCancel(currentUser)) {
            throw new IllegalStateException("Cannot cancel this transfer");
        }

        transfer.setStatus(SkillTransfer.TransferStatus.CANCELLED);
        transfer.setRespondedAt(Instant.now());
        SkillTransfer saved = transferRepository.save(transfer);

        log.info("Transfer cancelled: {} cancelled transfer of skill {}",
                currentUser.getHandle(), transfer.getSkill().getSlug());

        // 通知接收者
        webSocketHandler.notifyUser(transfer.getToUser().getId(), "TRANSFER_CANCELLED", Map.of(
                "transferId", transferId,
                "skillSlug", transfer.getSkill().getSlug()
        ));

        return saved;
    }

    /**
     * 清理过期的转移请求
     */
    @Transactional
    public void expireOldTransfers() {
        List<SkillTransfer> expired = transferRepository.findExpired(Instant.now());
        for (SkillTransfer transfer : expired) {
            if (transfer.getStatus() == SkillTransfer.TransferStatus.PENDING) {
                transfer.setStatus(SkillTransfer.TransferStatus.EXPIRED);
                transferRepository.save(transfer);
                log.info("Transfer expired: {} for skill {}",
                        transfer.getId(), transfer.getSkill().getSlug());
            }
        }
    }

    /**
     * 获取技能的待处理转移
     */
    @Transactional(readOnly = true)
    public Optional<SkillTransfer> getPendingTransferForSkill(Skill skill) {
        return transferRepository.findActiveBySkill(skill, Instant.now());
    }
}