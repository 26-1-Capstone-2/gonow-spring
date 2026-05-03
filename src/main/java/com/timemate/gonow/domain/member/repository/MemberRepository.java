package com.timemate.gonow.domain.member.repository;

import com.timemate.gonow.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 이메일/닉네임 찾기
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNickname(String nickname);

    // 중복 이메일/닉네임인지 체크
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
