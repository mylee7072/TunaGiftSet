package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.QuestionStatus;

@Entity
@Table(
        name = "product_questions",
        indexes = {
                @Index(name = "idx_product_questions_product_id", columnList = "product_id"),
                @Index(name = "idx_product_questions_member_id", columnList = "member_id"),
                @Index(name = "idx_product_questions_status", columnList = "status"),
                @Index(name = "idx_product_questions_created_at", columnList = "created_at")
        }
)
public class ProductQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_questions_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_questions_member"))
    private Member member;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean secret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.WAITING;

    @OneToOne(mappedBy = "question", fetch = FetchType.LAZY)
    private ProductQuestionAnswer answer;

    protected ProductQuestion() {
    }

    public ProductQuestion(Product product, Member member, String title, String content, boolean secret) {
        this.product = product;
        this.member = member;
        this.title = title;
        this.content = content;
        this.secret = secret;
    }

    public void update(String title, String content, boolean secret) {
        this.title = title;
        this.content = content;
        this.secret = secret;
    }

    public void markAnswered() {
        this.status = QuestionStatus.ANSWERED;
    }

    public void attachAnswer(ProductQuestionAnswer answer) {
        this.answer = answer;
    }

    public void delete() {
        this.status = QuestionStatus.DELETED;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Member getMember() {
        return member;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isSecret() {
        return secret;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public ProductQuestionAnswer getAnswer() {
        return answer;
    }
}
