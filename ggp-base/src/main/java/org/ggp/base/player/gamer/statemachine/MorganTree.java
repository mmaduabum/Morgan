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

public class MorganTree extends StateMachineGamer {



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
			selection = bestMove(getCurrentState());
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		//System.out.println("The move is:" + selection);

		start = System.currentTimeMillis();
		return selection;

	}

	private Move bestMove(MachineState state) {
		// TODO Auto-generated method stub
		return null;
	}

	private Move bestSPMove(MachineState state, long timeout)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();

		Move selection = null;
		if (root == null) {
			root = new treeNode(state);
		}
		int depth = 0;
		while (start + 2000 < timeout) {

			treeNode node = null;

			node = SPSelection(root);
			SPExpansion(node);
			Double reward = SPSimulation(node.current); //change from node created
			if (getStateMachine().findTerminalp(node.current)) {
				depth++;
//				System.out.println(node.current);
//				System.out.println(node.visit);
			}
			int score = reward.intValue();
			SPBackpropagation(node, score);
			start = System.currentTimeMillis();
		}
		int high_score = 0;
		treeNode newRoot = null;
		System.out.println("...");
		for (treeNode child : root.children) {
			System.out.println("move to this child: " + child.moveTo);
			System.out.println("utility: " + child.utility);
			System.out.println("visits: " + child.visit);
			if (child.utility/child.visit >= high_score) {
				selection = child.moveTo;
				newRoot = child;
				high_score = child.utility;
			}
		}
		root = newRoot;
		return selection;

	}

	private void SPBackpropagation(treeNode node, int score) {
		node.visit += 1;
		node.utility += score;
		if (node.parent != null) {
			SPBackpropagation(node.parent, score);
		}
	}

	private double SPSimulation(MachineState state)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.findTerminalp(state)) {
			//System.out.println(machine.findReward(getRole(), state));
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
		return SPSimulation(newState);

	}

	int count = 0;
	private void SPExpansion(treeNode node)
		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();

		if (!machine.findTerminalp(node.current)) {

			List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
			for (int i=0; i < moves.size(); i++) {
				Move move = moves.get(i);
				List<Role> roles = machine.getRoles();
				ArrayList<Move> new_moves = new ArrayList<Move>();
				new_moves.add(move);
				MachineState nextState = machine.getNextState(node.current, new_moves);
				treeNode newNode = new treeNode(nextState);
				newNode.parent = node;
				node.children.add(newNode);
				newNode.moveTo = move;

			}
		} else {
			count++;
		}
	}

	private treeNode SPSelection(treeNode node) {
		if (node.visit == 0 || node.children.size() == 0) {
			return node;
		}
		for (int i = 0; i < node.children.size(); i++) {
			if (node.children.get(i).visit == 0) {
				return node.children.get(i);
			}
		}
		double score = 0;
		treeNode result = null;
		for (int i = 0; i < node.children.size(); i++) {
			double newscore = selectFn(node.children.get(i));
			if (newscore >= score) {
				score = newscore;
				result = node.children.get(i);
			}
		}
		return SPSelection(result);
	}

	private double selectFn(treeNode node) {
		double uti = node.utility;
		double vis = node.visit;
		double par_vis = node.parent.visit;
		double res = uti/vis + 500 * Math.sqrt(2* Math.log(par_vis) / vis);
		return res;

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
		return "MorganTree";
	}

	private Role role;
	private MachineState currentState;
	private StateMachine stateMachine;
	private int globalLimit = 1;
	private int monteCarloCount = 4;
	private Double monteCarlo_weight = 0.5;
	private Double mobility_weight = 0.7;
	private Double focus_weight = 0.3;
	private Double goal_weight = .7;
	private treeNode root;

	private class treeNode {
		int visit = 0;
		int utility = 0;
		Move moveTo;

		treeNode parent = null;

		ArrayList<treeNode> children;
		MachineState current = null;

		private treeNode(MachineState state) {
			current = state;
			children = new ArrayList<treeNode>();
			moveTo = null;
		}

		private MachineState getState() {
			return current;
		}

		private int getVisits() {
			return visit;
		}

		private int getGoal() {
			return utility;
		}


	}
}
