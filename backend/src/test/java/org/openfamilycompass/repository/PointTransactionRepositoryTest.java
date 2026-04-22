package org.openfamilycompass.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.PointTransactionStatus;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the reservation semantics of
 * {@link PointTransactionRepository#sumPointsByUser(User)}:
 * <ul>
 *   <li>COMPLETED transactions always count.</li>
 *   <li>PENDING transactions with negative points count (reward reservation).</li>
 *   <li>PENDING transactions with non-negative points do NOT count (task awaiting
 *   parent approval).</li>
 *   <li>CANCELLED transactions do NOT count.</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PointTransactionRepository#sumPointsByUser - reservation semantics")
class PointTransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PointTransactionRepository repository;

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser("child");
        entityManager.persistAndFlush(user);
    }

    @Test
    @DisplayName("returns null when the user has no transactions")
    void noTransactions() {
        assertThat(repository.sumPointsByUser(user)).isNull();
    }

    @Test
    @DisplayName("sums COMPLETED transactions regardless of sign")
    void sumsCompleted() {
        persistTx(50, PointTransactionStatus.COMPLETED);
        persistTx(-20, PointTransactionStatus.COMPLETED);

        assertThat(repository.sumPointsByUser(user)).isEqualTo(30);
    }

    @Test
    @DisplayName("excludes a PENDING credit from a submitted task")
    void excludesPendingCredit() {
        persistTx(100, PointTransactionStatus.COMPLETED);
        persistTx(25, PointTransactionStatus.PENDING);

        assertThat(repository.sumPointsByUser(user)).isEqualTo(100);
    }

    @Test
    @DisplayName("includes a PENDING debit from a requested reward")
    void includesPendingReservation() {
        persistTx(100, PointTransactionStatus.COMPLETED);
        persistTx(-30, PointTransactionStatus.PENDING);

        assertThat(repository.sumPointsByUser(user)).isEqualTo(70);
    }

    @Test
    @DisplayName("excludes CANCELLED transactions")
    void excludesCancelled() {
        persistTx(100, PointTransactionStatus.COMPLETED);
        persistTx(-50, PointTransactionStatus.CANCELLED);

        assertThat(repository.sumPointsByUser(user)).isEqualTo(100);
    }

    @Test
    @DisplayName("mixed statuses: only COMPLETED + PENDING reservations count")
    void mixedStatuses() {
        persistTx(100, PointTransactionStatus.COMPLETED); // +100
        persistTx(-10, PointTransactionStatus.COMPLETED); // -10  -> running 90
        persistTx(42, PointTransactionStatus.PENDING);    // excluded (pending credit)
        persistTx(-30, PointTransactionStatus.PENDING);   // -30  -> running 60 (reservation)
        persistTx(-99, PointTransactionStatus.CANCELLED); // excluded

        assertThat(repository.sumPointsByUser(user)).isEqualTo(60);
    }

    private void persistTx(int points, PointTransactionStatus status) {
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setPoints(points);
        tx.setType(PointTransactionType.TASK);
        tx.setStatus(status);
        tx.setDescription("test");
        entityManager.persistAndFlush(tx);
    }

    private User createUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setFirstName("Test");
        u.setPassword("password");
        u.setRole(UserRole.CHILD);
        u.setActive(true);
        return u;
    }
}
