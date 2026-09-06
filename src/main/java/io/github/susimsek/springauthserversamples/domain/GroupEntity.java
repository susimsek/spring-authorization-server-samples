package io.github.susimsek.springauthserversamples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "groups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_seq")
    @SequenceGenerator(name = "group_seq", sequenceName = "group_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private GroupEntity parent;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
            name = "group_authorities",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id"))
    private Set<AuthorityEntity> authorities = new HashSet<>();
}
