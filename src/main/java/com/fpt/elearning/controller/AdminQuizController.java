package com.fpt.elearning.controller;

import com.fpt.elearning.entity.Choice;
import com.fpt.elearning.entity.Lesson;
import com.fpt.elearning.entity.Question;
import com.fpt.elearning.repository.LessonRepository;
import com.fpt.elearning.repository.QuestionRepository;
import com.fpt.elearning.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminQuizController {

    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuizService quizService;

    @GetMapping("/lessons/{lessonId}/questions")
    @Transactional(readOnly = true)
    public String questions(@PathVariable Long lessonId, Model model, RedirectAttributes ra) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            ra.addFlashAttribute("error", "Khong tim thay bai hoc");
            return "redirect:/admin/courses";
        }
        // Nap dap an de hien thi
        List<Question> questions = questionRepository.findByLesson_IdOrderByIdAsc(lessonId);
        questions.forEach(q -> q.getChoices().size());
        model.addAttribute("lesson", lesson);
        model.addAttribute("courseId", lesson.getCourse().getId());
        model.addAttribute("questions", questions);
        return "admin/questions";
    }

    /** Sinh cau hoi bang AI (Ollama) tu noi dung bai hoc */
    @PostMapping("/lessons/{lessonId}/generate")
    public String generate(@PathVariable Long lessonId,
                           @RequestParam(required = false) String instruction,
                           RedirectAttributes ra) {
        try {
            int n = quizService.generateForLesson(lessonId, instruction);
            ra.addFlashAttribute("success", "AI da tao " + n + " cau hoi. Hay kiem tra lai truoc khi xuat ban.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/lessons/" + lessonId + "/questions";
    }

    /** Them cau hoi thu cong (du phong khi AI loi) */
    @PostMapping("/lessons/{lessonId}/questions")
    public String addQuestion(@PathVariable Long lessonId,
                              @RequestParam String content,
                              @RequestParam(name = "choice", required = false) List<String> choices,
                              @RequestParam(defaultValue = "0") int correctIndex,
                              RedirectAttributes ra) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            ra.addFlashAttribute("error", "Khong tim thay bai hoc");
            return "redirect:/admin/courses";
        }
        if (choices == null) choices = List.of();

        Question q = Question.builder().lesson(lesson).content(content.trim()).build();
        for (int i = 0; i < choices.size(); i++) {
            String text = choices.get(i);
            if (text == null || text.isBlank()) continue;
            q.getChoices().add(Choice.builder()
                    .question(q).content(text.trim()).correct(i == correctIndex).build());
        }
        if (q.getChoices().size() < 2) {
            ra.addFlashAttribute("error", "Can it nhat 2 dap an.");
            return "redirect:/admin/lessons/" + lessonId + "/questions";
        }
        if (q.getChoices().stream().noneMatch(Choice::isCorrect)) {
            q.getChoices().get(0).setCorrect(true);
        }
        questionRepository.save(q);
        ra.addFlashAttribute("success", "Da them cau hoi.");
        return "redirect:/admin/lessons/" + lessonId + "/questions";
    }

    @PostMapping("/questions/{id}/delete")
    public String deleteQuestion(@PathVariable Long id,
                                 @RequestParam Long lessonId,
                                 RedirectAttributes ra) {
        questionRepository.deleteById(id);
        ra.addFlashAttribute("success", "Da xoa cau hoi.");
        return "redirect:/admin/lessons/" + lessonId + "/questions";
    }

    /** Admin tu chon lai dap an dung cho 1 cau hoi (sua sai cua AI) */
    @PostMapping("/questions/{questionId}/correct")
    @Transactional
    public String setCorrect(@PathVariable Long questionId,
                             @RequestParam Long choiceId,
                             @RequestParam Long lessonId,
                             RedirectAttributes ra) {
        Question q = questionRepository.findById(questionId).orElse(null);
        if (q != null) {
            q.getChoices().forEach(c -> c.setCorrect(c.getId().equals(choiceId)));
            questionRepository.save(q);
            ra.addFlashAttribute("success", "Đã cập nhật đáp án đúng.");
        }
        return "redirect:/admin/lessons/" + lessonId + "/questions";
    }
}
