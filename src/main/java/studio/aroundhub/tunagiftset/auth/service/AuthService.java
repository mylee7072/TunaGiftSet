package studio.aroundhub.tunagiftset.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.auth.dto.LoginRequest;
import studio.aroundhub.tunagiftset.auth.dto.LoginResponse;
import studio.aroundhub.tunagiftset.auth.dto.SignupRequest;
import studio.aroundhub.tunagiftset.auth.oauth.OAuthUserInfo;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.type.MemberProvider;
import studio.aroundhub.tunagiftset.entity.type.MemberStatus;
import studio.aroundhub.tunagiftset.exception.AuthenticationFailedException;
import studio.aroundhub.tunagiftset.exception.DuplicateResourceException;
import studio.aroundhub.tunagiftset.member.dto.MemberResponse;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("EMAIL_DUPLICATED", "이미 가입된 이메일입니다.");
        }

        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone()
        );

        return MemberResponse.from(memberRepository.save(member));
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .filter(foundMember -> foundMember.getStatus() == MemberStatus.ACTIVE)
                .filter(foundMember -> passwordEncoder.matches(request.password(), foundMember.getPassword()))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "LOGIN_FAILED",
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        String accessToken = jwtTokenProvider.createAccessToken(member);
        return LoginResponse.bearer(accessToken, jwtTokenProvider.getExpiresInSeconds(), MemberResponse.from(member));
    }

    @Transactional
    public LoginResponse oauthLogin(MemberProvider provider, OAuthUserInfo userInfo) {
        Member member = memberRepository.findByProviderAndProviderId(provider, userInfo.providerId())
                .orElseGet(() -> registerOAuthMember(provider, userInfo));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AuthenticationFailedException("LOGIN_FAILED", "이용이 제한된 계정입니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member);
        return LoginResponse.bearer(accessToken, jwtTokenProvider.getExpiresInSeconds(), MemberResponse.from(member));
    }

    private Member registerOAuthMember(MemberProvider provider, OAuthUserInfo userInfo) {
        if (memberRepository.existsByEmail(userInfo.email())) {
            // Same email already belongs to a LOCAL (or other-provider) account. We deliberately
            // don't auto-link accounts on a bare email match — that would let anyone with an
            // unverified-enough OAuth email take over an existing account.
            throw new DuplicateResourceException(
                    "EMAIL_DUPLICATED",
                    "이미 다른 방식으로 가입된 이메일입니다. 기존 로그인 방식을 이용해 주세요."
            );
        }
        Member member = Member.oauth(userInfo.email(), userInfo.name(), provider, userInfo.providerId());
        return memberRepository.save(member);
    }
}
