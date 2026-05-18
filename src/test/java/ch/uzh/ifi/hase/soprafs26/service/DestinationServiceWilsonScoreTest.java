package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.DestinationRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;

@DisplayName("DestinationService Wilson Score Tests")
class DestinationServiceWilsonScoreTest {

    private DestinationService destinationService;

    @BeforeEach
    void setup() {
        DestinationRepository destinationRepository = Mockito.mock(DestinationRepository.class);
        ActivityRepository activityRepository = Mockito.mock(ActivityRepository.class);
        VoteRepository voteRepository = Mockito.mock(VoteRepository.class);
        TripService tripService = Mockito.mock(TripService.class);

        destinationService = new DestinationService(
                destinationRepository,
                activityRepository,
                voteRepository,
                tripService);
    }

    @Test
    @DisplayName("Returns 0 for zero votes")
    void testZeroVotes() {
        double score = destinationService.testWilsonScore(0, 0);
        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("Single upvote is positive but below 1")
    void testSingleUpvote() {
        double score = destinationService.testWilsonScore(1, 0);
        assertTrue(score > 0.0);
        assertTrue(score < 1.0);
    }

    @Test
    @DisplayName("Single downvote stays close to zero")
    void testSingleDownvote() {
        double score = destinationService.testWilsonScore(0, 1);
        assertTrue(score >= 0.0);
        assertTrue(score < 0.15);
    }

    @Test
    @DisplayName("All upvotes with many votes is high but below 1")
    void testAllUpvotes() {
        double score = destinationService.testWilsonScore(100, 0);
        assertTrue(score > 0.85);
        assertTrue(score < 1.0);
    }

    @Test
    @DisplayName("All downvotes with many votes stays low")
    void testAllDownvotes() {
        double score = destinationService.testWilsonScore(0, 100);
        assertTrue(score >= 0.0);
        assertTrue(score <= 0.15);
    }

    @ParameterizedTest
    @CsvSource({
            "0,0",
            "1,0",
            "0,1",
            "10,10",
            "100,0",
            "0,100",
            "37,13",
            "999,1",
            "1,999"
    })
    @DisplayName("Wilson score always stays within [0, 1]")
    void testScoreBounds(long upvotes, long downvotes) {
        double score = destinationService.testWilsonScore(upvotes, downvotes);
        assertTrue(score >= 0.0);
        assertTrue(score <= 1.0);
    }

    @Test
    @DisplayName("Monotonicity: more upvotes increase score with fixed downvotes")
    void testMonotonicityUpvotes() {
        double score1 = destinationService.testWilsonScore(10, 5);
        double score2 = destinationService.testWilsonScore(20, 5);
        double score3 = destinationService.testWilsonScore(30, 5);

        assertTrue(score1 < score2);
        assertTrue(score2 < score3);
    }

    @Test
    @DisplayName("Monotonicity: more downvotes decrease score with fixed upvotes")
    void testMonotonicityDownvotes() {
        double score1 = destinationService.testWilsonScore(20, 10);
        double score2 = destinationService.testWilsonScore(20, 20);
        double score3 = destinationService.testWilsonScore(20, 30);

        assertTrue(score1 > score2);
        assertTrue(score2 > score3);
    }

    @Test
    @DisplayName("Larger samples produce higher confidence for strong positive ratios")
    void testLargerSampleConfidence() {
        double smallSample = destinationService.testWilsonScore(1, 0);
        double largeSample = destinationService.testWilsonScore(101, 1);

        assertTrue(largeSample > smallSample);
    }
}
