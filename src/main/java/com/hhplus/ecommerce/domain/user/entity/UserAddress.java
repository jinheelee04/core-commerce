package com.hhplus.ecommerce.domain.user.entity;

import com.hhplus.ecommerce.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "user_addresses",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_addresses_user_address_name",
            columnNames = {"user_id", "address_name"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_name", length = 50)
    private String addressName;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    // 비즈니스 로직용 생성자
    public UserAddress(User user, String addressName, String recipientName,
                       String recipientPhone, String postalCode, String address,
                       String addressDetail, Boolean isDefault) {
        this.user = user;
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.isDefault = isDefault != null ? isDefault : false;
    }

    // 주소 정보 수정
    public void updateAddress(String recipientName, String recipientPhone,
                              String postalCode, String address, String addressDetail) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

    // 기본 배송지 설정
    public void setAsDefault() {
        this.isDefault = true;
    }

    // 기본 배송지 해제
    public void unsetAsDefault() {
        this.isDefault = false;
    }
}