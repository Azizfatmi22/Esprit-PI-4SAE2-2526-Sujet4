package org.example.msevaluation.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.ExamQuestions;
import org.example.msevaluation.entities.QuizzQuestions;
import org.example.msevaluation.entities.TypeAssessment;
import org.example.msevaluation.repositories.EvaluationRepository;
import org.example.msevaluation.services.interfaces.IEvaluationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements IEvaluationService {
    private final EvaluationRepository evaluationRepository;

    public boolean isAccessAuthorized(Evaluation evaluation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = evaluation.getDate();
        LocalDateTime endTime = startTime.plusMinutes(evaluation.getDuration());
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    @Override
    @Transactional
    public void updateEvaluationDate(Long id, LocalDateTime newDate) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        evaluation.setDate(newDate);
    }

    @Override
    @Transactional
    public Evaluation saveEvaluation(Evaluation evaluation) {
        if (evaluation.getQuizzQuestions() != null) {
            evaluation.getQuizzQuestions().forEach(q -> {
                q.setEvaluation(evaluation);
                if (q.getQuizzAnswers() != null) {
                    q.getQuizzAnswers().forEach(a -> a.setQuizzQuestion(q));
                }
            });
        }
        if (evaluation.getExamQuestions() != null) {
            evaluation.getExamQuestions().forEach(q ->{
                q.setEvaluation(evaluation); // Lien Parent
                if (q.getKeywords() == null) {
                    q.setKeywords(new ArrayList<>());
                }
            });
        }
        return evaluationRepository.save(evaluation);
    }

    @Override
    @Transactional
    public Evaluation updateEvaluation(Long id, Evaluation updatedEval) {

        Evaluation existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));

        existing.setTitle(updatedEval.getTitle());
        existing.setDuration(updatedEval.getDuration());
        existing.setDate(updatedEval.getDate());
        existing.setMinSuccessScore(updatedEval.getMinSuccessScore());
        existing.setCourseId(updatedEval.getCourseId());
        existing.setTypeAssessment(updatedEval.getTypeAssessment());

        if (updatedEval.getTypeAssessment() == TypeAssessment.QUIZ) {
            existing.getExamQuestions().clear();
            existing.getQuizzQuestions().clear();
            if (updatedEval.getQuizzQuestions() != null) {
                for (QuizzQuestions newQ : updatedEval.getQuizzQuestions()) {
                    newQ.setEvaluation(existing);
                    if (newQ.getQuizzAnswers() != null) {
                        newQ.getQuizzAnswers().forEach(a -> a.setQuizzQuestion(newQ));
                    }
                    existing.getQuizzQuestions().add(newQ);
                }
            }
        } else {
            existing.getQuizzQuestions().clear();
            existing.getExamQuestions().clear();
            if (updatedEval.getExamQuestions() != null) {
                for (ExamQuestions newQ : updatedEval.getExamQuestions()) {
                    newQ.setEvaluation(existing);
                    existing.getExamQuestions().add(newQ);
                }
            }
        }

        return evaluationRepository.save(existing);
    }

    @Override
    public List<Evaluation> getAllEvaluations() {
        return evaluationRepository.findAll();
    }

    @Override
    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluation non trouvée avec l'id : " + id));
    }

    @Transactional
    @Override
    public void deleteEvaluation(Long id) {
        evaluationRepository.deleteById(id);
    }
    @Transactional
    @Override
    public Double getGlobalSuccessRate() {
        Double examRate = evaluationRepository.calculateAvgSuccessRateExams();
        Long correct = evaluationRepository.countCorrectQuizAnswers();
        Long total = evaluationRepository.countTotalQuizAnswers();
        Double quizRate = (total > 0) ? (correct.doubleValue() / total.doubleValue()) * 100 : 0.0;
        if (examRate == null) return quizRate;
        return (examRate + quizRate) / 2;
    }


}
