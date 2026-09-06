package studio.aroundhub.tunagiftset.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.member.dto.MemberResponse;
import studio.aroundhub.tunagiftset.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberResponse findMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."));

        return MemberResponse.from(member);
    }
}
