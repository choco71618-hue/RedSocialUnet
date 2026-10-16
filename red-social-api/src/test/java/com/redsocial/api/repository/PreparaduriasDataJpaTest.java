package com.redsocial.api.repository;

import com.redsocial.api.domain.Connection;
import com.redsocial.api.domain.Review;
import com.redsocial.api.domain.Subject;
import com.redsocial.api.domain.TutorSubject;
import com.redsocial.api.domain.User;
import com.redsocial.common.dto.ConnectionStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PreparaduriasDataJpaTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private SubjectRepository subjects;
    @Autowired
    private TutorSubjectRepository tutorSubjects;
    @Autowired
    private ConnectionRepository connections;
    @Autowired
    private ReviewRepository reviews;
    @Autowired
    private EntityManager em;

    private User user(String name, String role) {
        return users.save(User.builder()
                .username(name).email(name + "@unet.local").password("secret123")
                .displayName(name).role(role).build());
    }

    @Test
    void persisteConexionYResenaYCalculaPromedio() {
        User student = user("stu", "STUDENT");
        User tutor = user("tut", "TUTOR");
        Subject subject = subjects.save(Subject.builder().name("Matemática I").build());
        tutorSubjects.save(TutorSubject.builder()
                .tutor(tutor).subject(subject).pricePerHour(new BigDecimal("4000.00")).build());

        Connection connection = connections.save(Connection.builder()
                .student(student).tutor(tutor)
                .status(ConnectionStatus.ACCEPTED).reviewCode("ABC123").build());
        reviews.save(Review.builder()
                .connection(connection).student(student).tutor(tutor)
                .rating(4).comment("Muy claro").build());
        em.flush();

        assertThat(connections.findByStudent_IdAndTutor_Id(student.getId(), tutor.getId())).isPresent();
        assertThat(reviews.averageByTutor(tutor.getId())).isEqualTo(4.0);
        assertThat(tutorSubjects.findByTutor_IdOrderBySubject_NameAsc(tutor.getId())).hasSize(1);
        assertThat(subjects.findByNameIgnoreCase("matemática i")).isPresent();
    }

    @Test
    void noPermiteDosConexionesEntreLosMismosUsuarios() {
        User student = user("stu2", "STUDENT");
        User tutor = user("tut2", "TUTOR");
        connections.saveAndFlush(Connection.builder()
                .student(student).tutor(tutor)
                .status(ConnectionStatus.PENDING).reviewCode("AAA111").build());

        assertThatThrownBy(() -> connections.saveAndFlush(Connection.builder()
                .student(student).tutor(tutor)
                .status(ConnectionStatus.PENDING).reviewCode("BBB222").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void noPermiteDosResenasParaLaMismaConexion() {
        User student = user("stu3", "STUDENT");
        User tutor = user("tut3", "TUTOR");
        Connection connection = connections.saveAndFlush(Connection.builder()
                .student(student).tutor(tutor)
                .status(ConnectionStatus.ACCEPTED).reviewCode("CCC333").build());
        reviews.saveAndFlush(Review.builder()
                .connection(connection).student(student).tutor(tutor)
                .rating(5).comment("Excelente").build());

        assertThatThrownBy(() -> reviews.saveAndFlush(Review.builder()
                .connection(connection).student(student).tutor(tutor)
                .rating(3).comment("Otra").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
