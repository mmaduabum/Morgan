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
public class MorganCompulsiveGamer extends StateMachineGamer {


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
//		List<Move> moves = getStateMachine().findActions(getRole());
		int score = 0;
		for (int i=0; i < moves.size(); i++) {
			Move move = moves.get(i);
			List<Role> roles = machine.getRoles();
			ArrayList<Move> new_moves = new ArrayList<Move>();
			new_moves.add(move);

			MachineState nextState = machine.getNextState(state, new_moves);
			int result = maxScore(nextState);
			if (result >= 100) return result;
			if (result > score) score = result;
		}
		return score;
	}

	private Move bestMove(MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Move> moves = machine.getLegalMoves(state, getRole());
//		List<Move> moves = getStateMachine().findActions(getRole());
		System.out.println(moves.toString());
		int score = 0;
		Move move = moves.get(0);
		for (int i=0; i < moves.size(); i++) {
			Move currentMove = moves.get(i);
//			MachineState newState = machine.getNextState(state, machine.getRandomJointMove(getCurrentState(), getRole(), currentMove));

			List<Role> roles = machine.getRoles();
			ArrayList<Move> new_moves = new ArrayList<Move>();
			new_moves.add(currentMove);

			MachineState newState = machine.getNextState(state, new_moves);
			int result = maxScore(newState);
			System.out.println(result);
			if (result >= 100) return moves.get(i);
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
		return "Morgan Compulsive";
	}
	private Role role;
	private MachineState currentState;
	private StateMachine stateMachine;


}
