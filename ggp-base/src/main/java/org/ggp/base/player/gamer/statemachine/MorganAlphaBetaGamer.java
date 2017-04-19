package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/* This is the Morgan Freecow Legal Gamer
 *
 */
public class MorganAlphaBetaGamer extends StateMachineGamer {


	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}




	@Override
	public void stateMachineMetaGame(long timeout) {
	}

	private int maxScore(MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.findTerminalp(state)) {
			return machine.findReward(getRole(), state);
		}
		List<Move> moves = machine.getLegalMoves(state, getRole());
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);
			int result = minScore(move, state);
			if (result > score) {
				score = result;
			}
		}
		return score;
	}

	private int minScore(Move move, MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();


		List<Role> opponents = new ArrayList<Role>(machine.getRoles());
		opponents.remove(getRole());
		List<Move> moves = machine.getLegalMoves(state, opponents.get(0));
		int score = 100;
		for (int i = 0; i < moves.size(); i++) {
			Move opponent_move = moves.get(i);
			ArrayList<Move> moveset = new ArrayList<Move>();
			moveset.add(move);
			moveset.add(opponent_move);

			MachineState newState = machine.getNextState(state, moveset);
			int result = maxScore(newState);
			if (result < score) {
				score = result;
			}

		}
		return score;
	}

	private Move bestMove(MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Move> moves = machine.getLegalMoves(state, getRole());

		System.out.println(moves.toString());
		int score = 0;
		Move move = moves.get(0);
		for (int i=0; i < moves.size(); i++) {
			Move currentMove = moves.get(i);

			int result = minScore(currentMove, state);
			System.out.println(result);
			if (result >= score) {
				score = result;
				move = moves.get(i);
			}
		}
		return move;
	}
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = bestMove(getCurrentState());
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;

	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}


	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Morgan Alpha Beta";
	}
	private Role role;
	private MachineState currentState;
	private StateMachine stateMachine;


}
