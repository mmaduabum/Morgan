package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

/* This is the Morgan Freecow Alpha Beta Gamer
 *
 */
public class MorganH extends StateMachineGamer {


	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}


	private int mobilityHeuristic(Role role, MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Move> moves = machine.getLegalMoves(state, getRole());
		List<Move> feasibles = getStateMachine().findActions(getRole());
		Double res = ((moves.size()/feasibles.size()) * 100.0);
		return res.intValue();
	}

	private int focusHeuristic(Role role, MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Move> moves = machine.getLegalMoves(state, getRole());
		List<Move> feasibles = getStateMachine().findActions(getRole());
		Double res = ((100 - moves.size()/feasibles.size()) * 100.0);
		return res.intValue();
	}

	private int oppFocusHeuristic(Role role, MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Role> opponents = new ArrayList<Role>(machine.getRoles());
		opponents.remove(getRole());
		if (opponents.size() > 0) {
			List<Move> moves = machine.getLegalMoves(state, opponents.get(0));
			List<Move> feasibles = getStateMachine().findActions(opponents.get(0));
			Double res = ((100 - moves.size()/feasibles.size()) * 100.0);
			return res.intValue();
		}
		return 0;
	}
	private int goalProximityHeuristic(Role role, MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		return machine.getGoal(state, getRole());
	}

	private int evalfn(Role role, MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		int mh = mobilityHeuristic(role, state);
		int ofh = oppFocusHeuristic(role, state);
		//int mc = monteCarlo(state);
		int gl = goalProximityHeuristic(role, state);
		Double res = gl * .8  + ofh * .3 + mh * .5;
		return res.intValue();
	}

	@Override
	public void stateMachineMetaGame(long timeout) {
	}


	private int maxSPScore(MachineState state, int level)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.findTerminalp(state)) {
			return machine.findReward(getRole(), state);
		}
		if (level > globalLimit) {
			return mobilityHeuristic(getRole(), state);
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
			int result = maxSPScore(nextState, level+1);
			if (result >= 100) return result;
			if (result > score) score = result;
		}
		return score;
	}

	private Move bestSPMove(MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Move> moves = machine.getLegalMoves(state, getRole());
//		List<Move> moves = getStateMachine().findActions(getRole());
		//System.out.println(moves.toString());
		int score = 0;
		Move move = moves.get(0);
		for (int i=0; i < moves.size(); i++) {
			Move currentMove = moves.get(i);
//			MachineState newState = machine.getNextState(state, machine.getRandomJointMove(getCurrentState(), getRole(), currentMove));

			List<Role> roles = machine.getRoles();
			ArrayList<Move> new_moves = new ArrayList<Move>();
			new_moves.add(currentMove);

			MachineState newState = machine.getNextState(state, new_moves);
			int result = maxSPScore(newState, 0);
			//System.out.println(result);
			if (result >= 100) return moves.get(i);
			if (result >= score) {
				score = result;
				move = moves.get(i);
			}
		}
		return move;

	}

	private double depthCharge(MachineState state)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.findTerminalp(state)) {
			return machine.findReward(getRole(), state);
		}
		List<Role> roles = new ArrayList<Role>(machine.getRoles());
		int numRoles = roles.size();

		ArrayList<Move> moveSet = new ArrayList<Move>(numRoles);
		for (int i = 0; i < numRoles; i++) {
			List<Move> moves = machine.getLegalMoves(state, roles.get(i));
			Random rand = new Random();
			int n = rand.nextInt(moves.size());
			Move best = moves.get(n);
			moveSet.add(best);
		}
		MachineState newState = machine.getNextState(state, moveSet);
		return depthCharge(newState);

	}

	private int monteCarlo(MachineState state)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		double total = 0;
		for (int i=0; i < monteCarloCount; i++) {
			total = total + depthCharge(state);
		}
		return (int)total/monteCarloCount;
	}


	private int maxScore(MachineState state, int alpha, int beta, int level) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.findTerminalp(state)) {
			return machine.findReward(getRole(), state);
		}
		if (level > globalLimit) {
//			return evalfun(getRole(), state);
			//System.out.println("goal is" + machine.getGoal(state, getRole()));
			//Double res = 1.0 * machine.getGoal(state, getRole());
			//Double res = 1.0 * monteCarlo(state);
			//System.out.println("res: " + res);
			return evalfn(getRole(), state);
			//return res.intValue();
		}
		List<Move> moves = machine.getLegalMoves(state, getRole());
		for (int i = 0; i < moves.size(); i++) {
			int result = minScore(moves.get(i), state, alpha, beta, level);
			alpha = Math.max(alpha, result);
			if (alpha >= beta) {
				return beta;
			}

		}
		return alpha;
	}

	private int minScore(Move move, MachineState state, int alpha, int beta, int level) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Role> opponents = new ArrayList<Role>(machine.getRoles());
		opponents.remove(getRole());
		List<Move> moves = machine.getLegalMoves(state, opponents.get(0));
		for (int i = 0; i < moves.size(); i++) {
			Move opponent_move = moves.get(i);
			ArrayList<Move> moveset = new ArrayList<Move>();
			moveset.add(move);
			moveset.add(opponent_move);

			MachineState newState = machine.getNextState(state, moveset);

			int max_val = maxScore(newState, alpha, beta, level+1);
			beta = Math.min(beta, max_val);
			if (beta <= alpha) {
				return alpha;
			}
		}
		return beta;
	}

	private Move bestMove(MachineState state) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		List<Move> moves = machine.getLegalMoves(state, getRole());

		//System.out.println(moves.toString());
		int score = 0;
		Move move = moves.get(0);
		for (int i=0; i < moves.size(); i++) {
			Move currentMove = moves.get(i);

			int result = minScore(currentMove, state, 0, 100, 0);
			//System.out.println(result);
			if (result >= score) {
				score = result;
				move = moves.get(i);
			}
		}
		//System.out.println("Score is: " + score);
		return move;
	}
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection;
		if (getStateMachine().findRoles().size() == 1) {
			selection = bestSPMove(getCurrentState());
		} else {
			selection = bestMove(getCurrentState());
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		//System.out.println("The move is:" + selection);
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
		return "MorganH";
	}
	private Role role;
	private MachineState currentState;
	private StateMachine stateMachine;
	private int globalLimit = 3;
	private int monteCarloCount = 4;
	private Double monteCarlo_weight = 0.5;
	private Double mobility_weight = 0.7;
	private Double focus_weight = 0.3;
	private Double goal_weight = .7;


}