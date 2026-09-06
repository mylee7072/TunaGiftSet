package studio.aroundhub.tunagiftset.security;

import studio.aroundhub.tunagiftset.entity.type.MemberRole;

public record AuthMember(
        Long id,
        String email,
        MemberRole role
) {
}
