package beyond.orderSystem.member.domain;

import beyond.orderSystem.common.domain.Address;
import beyond.orderSystem.common.domain.BaseTimeEntity;
import beyond.orderSystem.member.dto.MemberResDto;
import lombok.*;

import javax.persistence.*;


@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    private String password;

    @Embedded //
    private Address address;

    @Enumerated(EnumType.STRING) // 이거 안하면 숫자로 들어감
    @Builder.Default
    private Role role = Role.USER;

    public MemberResDto fromEntity(){
        return MemberResDto.builder()
                        .id(this.id)
                        .name(this.name)
                        .email(this.email)
                        .address(this.address)
                .build();
    }

}
