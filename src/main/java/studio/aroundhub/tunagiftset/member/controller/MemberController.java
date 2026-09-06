package studio.aroundhub.tunagiftset.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.member.dto.MemberResponse;
import studio.aroundhub.tunagiftset.member.service.MemberService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public MemberResponse findMe(@AuthenticationPrincipal AuthMember authMember) {
        return memberService.findMe(authMember.id());
    }
}
