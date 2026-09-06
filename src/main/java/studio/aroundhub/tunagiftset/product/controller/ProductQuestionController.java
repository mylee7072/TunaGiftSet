package studio.aroundhub.tunagiftset.product.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.entity.type.MemberRole;
import studio.aroundhub.tunagiftset.entity.type.QuestionStatus;
import studio.aroundhub.tunagiftset.product.dto.QuestionAnswerRequest;
import studio.aroundhub.tunagiftset.product.dto.QuestionCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.QuestionPageResponse;
import studio.aroundhub.tunagiftset.product.dto.QuestionResponse;
import studio.aroundhub.tunagiftset.product.dto.QuestionUpdateRequest;
import studio.aroundhub.tunagiftset.product.service.ProductQuestionService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api")
public class ProductQuestionController {

    private final ProductQuestionService questionService;

    public ProductQuestionController(ProductQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/products/{productId}/questions")
    public QuestionPageResponse findProductQuestions(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentMemberId = authMember == null ? null : authMember.id();
        MemberRole role = authMember == null ? null : authMember.role();
        return questionService.findProductQuestions(productId, currentMemberId, role, page, size);
    }

    @GetMapping("/questions/{questionId}")
    public QuestionResponse findQuestion(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long questionId
    ) {
        Long currentMemberId = authMember == null ? null : authMember.id();
        MemberRole role = authMember == null ? null : authMember.role();
        return questionService.findQuestion(questionId, currentMemberId, role);
    }

    @PostMapping("/products/{productId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @Valid @RequestBody QuestionCreateRequest request
    ) {
        return questionService.create(authMember.id(), productId, request);
    }

    @GetMapping("/members/me/questions")
    public QuestionPageResponse findMyQuestions(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return questionService.findMyQuestions(authMember.id(), page, size);
    }

    @PutMapping("/questions/{questionId}")
    public QuestionResponse update(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateRequest request
    ) {
        return questionService.update(authMember.id(), questionId, request);
    }

    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long questionId
    ) {
        questionService.delete(authMember.id(), questionId);
    }

    @GetMapping("/admin/questions")
    public QuestionPageResponse adminFindQuestions(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) Boolean secret,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return questionService.adminFindQuestions(productId, keyword, status, secret, page, size);
    }

    @PostMapping("/admin/questions/{questionId}/answer")
    public QuestionResponse answer(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionAnswerRequest request
    ) {
        return questionService.answer(authMember.id(), questionId, request);
    }

    @PutMapping("/admin/questions/{questionId}/answer")
    public QuestionResponse updateAnswer(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionAnswerRequest request
    ) {
        return questionService.answer(authMember.id(), questionId, request);
    }
}
