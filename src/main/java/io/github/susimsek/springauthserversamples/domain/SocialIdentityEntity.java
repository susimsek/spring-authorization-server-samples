package io.github.susimsek.springauthserversamples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A stable link between an external OAuth2 identity and a local user. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "social_identities",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_social_identities_provider_subject",
                        columnNames = {"provider", "subject"}))
public class SocialIdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "social_identity_seq")
    @SequenceGenerator(
            name = "social_identity_seq",
            sequenceName = "social_identity_seq",
            allocationSize = 1)
    private Long id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public SocialIdentityEntity(String provider, String subject, UserEntity user) {
        this.provider = provider;
        this.subject = subject;
        this.user = user;
    }
}
