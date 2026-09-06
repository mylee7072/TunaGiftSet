package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;

@Entity
@Table(
        name = "product_question_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_question_answers_question_id", columnNames = "question_id")
        }
)
public class ProductQuestionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_question_answers_question"))
    private ProductQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_question_answers_admin_member"))
    private Member adminMember;

    @Column(nullable = false, length = 2000)
    private String content;

    protected ProductQuestionAnswer() {
    }

    public ProductQuestionAnswer(ProductQuestion question, Member adminMember, String content) {
        this.question = question;
        this.adminMember = adminMember;
        this.content = content;
    }

    public void update(String content) {
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public ProductQuestion getQuestion() {
        return question;
    }

    public Member getAdminMember() {
        return adminMember;
    }

    public String getContent() {
        return content;
    }
}
