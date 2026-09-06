package studio.aroundhub.tunagiftset.admin.order.dto;

import studio.aroundhub.tunagiftset.entity.Member;

public record AdminOrderMemberResponse(
        Long id,
        String email,
        String name,
        String phone
) {
    public static AdminOrderMemberResponse from(Member member) {
        return new AdminOrderMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhone());
    }
}
