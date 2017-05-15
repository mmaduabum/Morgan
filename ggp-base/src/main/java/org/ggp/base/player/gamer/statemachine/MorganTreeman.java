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

public class MorganTreeman extends StateMachineGamer {

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//
		StateMachine machine = getStateMachine();
		long start = System.currentTimeMillis();
		Move selection = null;


		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		if (getStateMachine().findRoles().size() == 1) {
			selection = bestSPMove(getCurrentState(), timeout);
		} else {
			selection = bestMove(getCurrentState(), timeout);
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		//System.out.println("The move is:" + selection);

		start = System.currentTimeMillis();
		return selection;

	}

	private MonteMPNode MPSelect(MonteMPNode node)
		throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
	List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
	List<List<Move>> jointMoves = new ArrayList();
	for (Move move : moves) {
		jointMoves.addAll(getStateMachine().getLegalJointMoves(node.current, getRole(), move));
	}


	// mp select, first legal moves to get min nodes
	// then legal joint moves



	if (node.grandchildren.size() < jointMoves.size() || node.visit == 0) {
		return node;
	}
	double score = 0;
	MonteMPNode result = null;




	for (int i = 0; i < node.grandchildren.size(); i++) {
		double newscore = MPSelectFn(node.grandchildren.get(i));
		if (newscore >= score) {
			score = newscore;
			result = node.grandchildren.get(i);
		}
	}
	return MPSelect(result);
	}

	private MonteMPNode MPExpand(MonteMPNode node)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {

	}


	private double MPSelectFn(MonteMPNode node) {
		// TODO Auto-generated method stub
		if (node.isMax) {

		} else {

		}
	}

	private Move bestMove(MachineState state, long timeout)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		long start = System.currentTimeMillis();
		Move selection = null;

		if (MProot == null) {
			MProot = new MonteMPNode(state, true);
		}
		while (start + 2000 < timeout) {
			MonteMPNode node  = MPSelect(MProot);

			start = System.currentTimeMillis();
		}


		return null;
	}



	private Move bestSPMove(MachineState state, long timeout)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		long start  = System.currentTimeMillis();

		if (Sproot == null) {
			Sproot = new MonteNode(state);
		}
		Move selection = null;
		while (start + 2000 < timeout) {

			MonteNode node = SPSelect(Sproot);
			MonteNode child = SPExpand(node);
			double reward = SPSimulate(child.current);
			SPBackpropagate(reward, child);


			start = System.currentTimeMillis();
		}
		double high_score = 0;
		MonteNode newRoot = null;
		System.out.println("...");
		for (MonteNode child : Sproot.children) {
			System.out.println("move to this child: " + child.moveTo);
			System.out.println("utility: " + child.utility);
			System.out.println("visits: " + child.visit);
			if (child.utility/child.visit >= high_score) {
				selection = child.moveTo;
				newRoot = child;
				high_score = (child.utility/child.visit);
			}
		}
		newRoot.parent = null;
		Sproot = newRoot;

		return selection;

	}

	private MonteNode SPSelect(MonteNode node)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
		if (node.children.size() < moves.size() || node.visit == 0) {
			return node;
		}
		double score = 0;
		MonteNode result = null;
		for (int i = 0; i < node.children.size(); i++) {
			double newscore = selectFn(node.children.get(i));
			if (newscore >= score) {
				score = newscore;
				result = node.children.get(i);
			}
		}
		return SPSelect(result);
	}

	private double selectFn(MonteNode node) {
		double uti = node.utility;
		double vis = node.visit;
		double par_vis = node.parent.visit;
		double res = uti/vis + 50 * Math.sqrt(Math.log(par_vis) / vis);
		return res;
	}

	private MonteNode SPExpand(MonteNode node)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();

		if (!machine.findTerminalp(node.current)) {

			List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
			int existingChildren = node.children.size();
			Move move = moves.get(existingChildren);
			ArrayList<Move> new_moves = new ArrayList<Move>();
			new_moves.add(move);
			MachineState nextState = machine.getNextState(node.current, new_moves);
			MonteNode newNode = new MonteNode(nextState);
			newNode.parent = node;
			node.children.add(newNode);
			newNode.moveTo = move;
			return newNode;
		} else {
			return node;
//			count++;
		}

	}


	private int SPSimulate(MachineState state)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.findTerminalp(state)) {
			return machine.findReward(getRole(), state);
		}
		List<Role> roles = new ArrayList<Role>(machine.getRoles());
		int numRoles = roles.size();
		ArrayList<Move> moveSet = new ArrayList<Move>(numRoles);
		List<Move> moves = machine.getLegalMoves(state, getRole());
		Random rand = new Random();
		int n = rand.nextInt(moves.size());
		Move best = moves.get(n);
		moveSet.add(best);

		MachineState newState = machine.getNextState(state, moveSet);
		return SPSimulate(newState);

	}

	private void SPBackpropagate(double reward, MonteNode node) {
		node.visit += 1;
		node.utility += reward;
		if (node.parent != null) {
			SPBackpropagate(reward, node.parent);
		}

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
		return "Morgan Treeman";
	}
	//Note MonteNode is only for single player
	private class MonteNode {
		double utility = 0;
		double visit = 0;
		MachineState current = null;
		MonteNode parent = null;
		Move moveTo;
		ArrayList<MonteNode> children;


		private MonteNode(MachineState state) {
			current = state;
			visit = 0; // should this be 0 or 1
			moveTo = null;
			children = new ArrayList<MonteNode>();

		}
	}



	private class MonteMPNode {
		double utility = 0;
		double visit = 0;
		MachineState current = null;
		MonteMPNode parent = null;
		Boolean isMax;
		ArrayList<MonteMPNode> grandchildren;

		private MonteMPNode(MachineState state, Boolean max) {
			utility = 0;
			visit = 0;
			current = state;
			isMax = max;
			grandchildren = new ArrayList<MonteMPNode>();
		}
	}

	MonteNode Sproot = null;
	MonteMPNode MProot = null;

}
