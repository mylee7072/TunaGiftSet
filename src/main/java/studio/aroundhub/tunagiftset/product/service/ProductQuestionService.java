package studio.aroundhub.tunagiftset.product.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.ProductQuestion;
import studio.aroundhub.tunagiftset.entity.ProductQuestionAnswer;
import studio.aroundhub.tunagiftset.entity.type.MemberRole;
import studio.aroundhub.tunagiftset.entity.type.QuestionStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.QuestionAnswerRequest;
import studio.aroundhub.tunagiftset.product.dto.QuestionCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.QuestionPageResponse;
import studio.aroundhub.tunagiftset.product.dto.QuestionResponse;
import studio.aroundhub.tunagiftset.product.dto.QuestionUpdateRequest;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.ProductQuestionAnswerRepository;
import studio.aroundhub.tunagiftset.repository.ProductQuestionRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductQuestionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ProductQuestionRepository questionRepository;
    private final ProductQuestionAnswerRepository answerRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public ProductQuestionService(
            ProductQuestionRepository questionRepository,
            ProductQuestionAnswerRepository answerRepository,
            ProductRepository productRepository,
            MemberRepository memberRepository
    ) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public QuestionResponse create(Long memberId, Long productId, QuestionCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product was not found."));
        Member member = getMember(memberId);
        ProductQuestion question = new ProductQuestion(
                product,
                member,
                normalizeText(request.title(), MAX_TITLE_LENGTH, "INVALID_QUESTION_TITLE", "Question title is required."),
                normalizeText(request.content(), MAX_CONTENT_LENGTH, "INVALID_QUESTION_CONTENT", "Question content is required."),
                request.secret()
        );
        return QuestionResponse.from(questionRepository.save(question), memberId, false);
    }

    public QuestionPageResponse findProductQuestions(Long productId, Long currentMemberId, MemberRole role, int page, int size) {
        ensureProductExists(productId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean admin = role == MemberRole.ADMIN;
        return QuestionPageResponse.from(questionRepository
                .findAllByProductIdAndStatusNot(productId, QuestionStatus.DELETED, pageable)
                .map(question -> QuestionResponse.from(question, currentMemberId, admin)));
    }

    public QuestionResponse findQuestion(Long questionId, Long currentMemberId, MemberRole role) {
        boolean admin = role == MemberRole.ADMIN;
        return QuestionResponse.from(getActiveQuestion(questionId), currentMemberId, admin);
    }

    public QuestionPageResponse findMyQuestions(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        return QuestionPageResponse.from(questionRepository
                .findAllByMemberIdAndStatusNot(memberId, QuestionStatus.DELETED, pageable)
                .map(question -> QuestionResponse.from(question, memberId, false)));
    }

    @Transactional
    public QuestionResponse update(Long memberId, Long questionId, QuestionUpdateRequest request) {
        ProductQuestion question = getActiveQuestion(questionId);
        if (!question.getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("QUESTION_NOT_FOUND", "Question was not found.");
        }
        if (question.getStatus() == QuestionStatus.ANSWERED) {
            throw new InvalidRequestException("QUESTION_NOT_EDITABLE", "Answered question cannot be edited.");
        }
        question.update(
                normalizeText(request.title(), MAX_TITLE_LENGTH, "INVALID_QUESTION_TITLE", "Question title is required."),
                normalizeText(request.content(), MAX_CONTENT_LENGTH, "INVALID_QUESTION_CONTENT", "Question content is required."),
                request.secret()
        );
        return QuestionResponse.from(question, memberId, false);
    }

    @Transactional
    public void delete(Long memberId, Long questionId) {
        ProductQuestion question = getActiveQuestion(questionId);
        if (!question.getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("QUESTION_NOT_FOUND", "Question was not found.");
        }
        question.delete();
    }

    public QuestionPageResponse adminFindQuestions(Long productId, String keyword, QuestionStatus status, Boolean secret, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<ProductQuestion> specification = productEquals(productId)
                .and(keywordContains(keyword))
                .and(statusEquals(status))
                .and(secretEquals(secret));
        return QuestionPageResponse.from(questionRepository.findAll(specification, pageable)
                .map(question -> QuestionResponse.from(question, null, true)));
    }

    @Transactional
    public QuestionResponse answer(Long adminMemberId, Long questionId, QuestionAnswerRequest request) {
        ProductQuestion question = getActiveQuestion(questionId);
        Member admin = getMember(adminMemberId);
        String content = normalizeText(request.content(), MAX_CONTENT_LENGTH, "INVALID_QUESTION_ANSWER", "Answer content is required.");
        ProductQuestionAnswer answer = answerRepository.findByQuestionId(questionId)
                .orElseGet(() -> answerRepository.save(new ProductQuestionAnswer(question, admin, content)));
        answer.update(content);
        question.markAnswered();
        question.attachAnswer(answer);
        return QuestionResponse.from(question, adminMemberId, true);
    }

    private ProductQuestion getActiveQuestion(Long questionId) {
        return questionRepository.findByIdAndStatusNot(questionId, QuestionStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("QUESTION_NOT_FOUND", "Question was not found."));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member was not found."));
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product was not found.");
        }
    }

    private String normalizeText(String value, int maxLength, String code, String blankMessage) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new InvalidRequestException(code, blankMessage);
        }
        if (normalized.length() > maxLength) {
            throw new InvalidRequestException(code, "Text is too long.");
        }
        return normalized;
    }

    private Specification<ProductQuestion> productEquals(Long productId) {
        return (root, query, cb) -> productId == null ? cb.conjunction() : cb.equal(root.get("product").get("id"), productId);
    }

    private Specification<ProductQuestion> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("content")), like));
        };
    }

    private Specification<ProductQuestion> statusEquals(QuestionStatus status) {
        return (root, query, cb) -> status == null ? cb.notEqual(root.get("status"), QuestionStatus.DELETED) : cb.equal(root.get("status"), status);
    }

    private Specification<ProductQuestion> secretEquals(Boolean secret) {
        return (root, query, cb) -> secret == null ? cb.conjunction() : cb.equal(root.get("secret"), secret);
    }
}
