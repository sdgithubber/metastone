package net.pferdimanzug.hearthstone.analyzer.game.behaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.pferdimanzug.hearthstone.analyzer.game.GameContext;
import net.pferdimanzug.hearthstone.analyzer.game.Player;
import net.pferdimanzug.hearthstone.analyzer.game.actions.GameAction;
import net.pferdimanzug.hearthstone.analyzer.game.cards.Card;
import net.pferdimanzug.hearthstone.analyzer.game.logic.GameLogic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlatMonteCarlo extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(FlatMonteCarlo.class);

	private int iterations;

	public FlatMonteCarlo(int iterations) {
		this.iterations = iterations;
	}

	private GameAction getBestAction(HashMap<GameAction, Integer> actionScores) {
		GameAction bestAction = null;
		int bestScore = Integer.MIN_VALUE;
		for (GameAction actionEntry : actionScores.keySet()) {
			int score = actionScores.get(actionEntry);
			if (score > bestScore) {
				bestAction = actionEntry;
				bestScore = score;
			}
		}
		logger.debug("Best action determined by MonteCarlo: " + bestAction.getActionType());
		return bestAction;
	}

	@Override
	public String getName() {
		return "Flat Monte-Carlo " + iterations;
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		List<Card> discardedCards = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getBaseManaCost() >= 4) {
				discardedCards.add(card);
			}
		}
		return discardedCards;
	}

	private int playRandomUntilEnd(GameContext simulation, int playerId) {
		for (Player player : simulation.getPlayers()) {
			player.setBehaviour(new PlayRandomBehaviour());
		}

		simulation.playTurn();
		return simulation.getWinningPlayerId() == playerId ? 1 : 0;
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1) {
			return validActions.get(0);
		}
		GameLogic.logger.debug("********SIMULATION starts**********");
		HashMap<GameAction, Integer> actionScores = new HashMap<>();
		for (GameAction gameAction : validActions) {
			int score = simulate(context, player.getId(), gameAction);
			actionScores.put(gameAction, score);
			logger.debug("Action {} gets score of {}", gameAction.getActionType(), score);

		}
		GameLogic.logger.debug("********SIMULATION ENDS**********");
		GameAction bestAction = getBestAction(actionScores);
		return bestAction;
	}

	private int simulate(GameContext context, int playerId, GameAction action) {
		GameContext simulation = context.clone();
		GameLogic.logger.debug("********TESTING FOLLOWING ACTION IN MONTE CARLO**********");
		simulation.getLogic().performGameAction(simulation.getActivePlayer().getId(), action);
		int score = 0;
		for (int i = 0; i < iterations; i++) {
			score += playRandomUntilEnd(simulation.clone(), playerId);
		}
		return score;
	}

}