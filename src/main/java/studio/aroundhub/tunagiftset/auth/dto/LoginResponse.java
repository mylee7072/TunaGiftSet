package studio.aroundhub.tunagiftset.auth.dto;

import studio.aroundhub.tunagiftset.member.dto.MemberResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        MemberResponse member
) {
    public static LoginResponse bearer(String accessToken, long expiresIn, MemberResponse member) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, member);
    }
}
