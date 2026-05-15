package com.timemate.gonow.domain.member.repository;

import com.timemate.gonow.domain.member.entity.MemberSetting;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberSettingRepository extends JpaRepository<MemberSetting, Long> {
    // member_id(FK) 찾기
    Optional<MemberSetting> findByMemberId(Long memberId);

    // member.email, member.nickname, member.home 등 사용 → member fetch join
    @EntityGraph(attributePaths = "member")
    Optional<MemberSetting> findWithMemberByMemberId(Long memberId);
}
