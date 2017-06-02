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
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public class MorganTreeman extends StateMachineGamer {

	@Override
	public StateMachine getInitialStateMachine() {
//		return new SamplePropNetStateMachine();
		machine = new SamplePropNetStateMachine();
		//machine = new CachedStateMachine(new ProverStateMachine());
		return machine;
	}


	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
//		StateMachine machine = getStateMachine();
//		System.out.println("here");
//		propnet = new SamplePropNetStateMachine();
//		propnet.initialize(getMatch().getGame().getRules());
//		System.out.println("initialized");

//		long start = System.currentTimeMillis();
//		while (start + 10000 < timeout) {
//			start = System.currentTimeMillis();
//		}
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

	private MaxNode MPSelect(MaxNode node) {
		if (node.visits == 0) {
			return node;
		}
		if (!node.hasAllGrandChildren) {
			return node;
		}
		MinNode maxOfMins = null;
		double maximum = 0.0;
		for (int i = 0; i < node.children.size(); i++) {
			MinNode child = node.children.get(i);
			double score = MPSelectFN(child);
			if (score >= maximum) {
				maxOfMins = child;
				maximum = score;
			}
		}
		for (int i = 0; i < maxOfMins.children.size(); i++) {
			MaxNode child = maxOfMins.children.get(i);
			double score = MPSelectFN(child);
			if (score >= maximum || i == 0) {
				node = child;
				maximum = score;
			}
		}
		return MPSelect(node);
	}

	private double MPSelectFN(MonteMPNode node) {
		double uti = node.utility;
		double vis = node.visits;
		double par_vis = node.parent.visits;
		if (node instanceof MaxNode) {
			double res = -1.0 * uti / vis + 50 * Math.sqrt(Math.log(par_vis) / vis);
			return res;
		} else if (node instanceof MinNode) {
			double res = uti / vis + 50 * Math.sqrt(Math.log(par_vis) / vis);
			return res;
		} else {
			System.out.println("HELP! Wrong!");
			return 0.0;
		}
	}

	private MaxNode MPExpand(MaxNode node) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(node.current)) {
			return node;
		}
		List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
		int amount_of_min_nodes = node.children.size();
		MinNode ExpandedMin;
		if (amount_of_min_nodes == 0) { //makes sure at leas one min node in list
			ExpandedMin = new MinNode(node, moves.get(0));
			node.children.add(ExpandedMin);
			amount_of_min_nodes = 1;
		}
		ExpandedMin = node.children.get(amount_of_min_nodes - 1);
		if (ExpandedMin.hasAllChildren) { //make new min node and add grandchild
			ExpandedMin = new MinNode(node, moves.get(amount_of_min_nodes)); //not -1 because new min node and index starts at 0
			node.children.add(ExpandedMin);
		} else {
			ExpandedMin = node.children.get(amount_of_min_nodes - 1);
		}
		//now add grandchild
		List<List<Move>> jointmoves = getStateMachine().getLegalJointMoves(node.current, getRole(), ExpandedMin.moveTo);
		List<Move> jmove = jointmoves.get(ExpandedMin.children.size());
		MachineState newState = machine.getNextState(node.current, jmove);
		MaxNode newestGrandChild = new MaxNode(newState, ExpandedMin);
		ExpandedMin.children.add(newestGrandChild);
		if (ExpandedMin.children.size() == jointmoves.size()) {
			ExpandedMin.hasAllChildren = true;
			if (node.children.size() == moves.size()) {
				node.hasAllGrandChildren = true;
			}
		}
		return newestGrandChild;
	}


	private Double MPSimulate(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(state)) {
			return (double) machine.findReward(getRole(), state);
		}
		List< List<Move> > jointmoves = machine.getLegalJointMoves(state);
		Random rand = new Random();
		int n = rand.nextInt(jointmoves.size());
		return MPSimulate(machine.getNextState(state, jointmoves.get(n)));
	}

	private void MPBackpropagate(MonteMPNode node, double reward) {
		node.visits += 1;
		node.utility += reward;
		if (node.parent != null) {
			MPBackpropagate(node.parent, reward);
		}

	}

	private Move bestMove(MachineState state, long timeout)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		long start = System.currentTimeMillis();
		Move selection = null;
		if (MProot == null) {
			MProot = new MaxNode(state, null);
		} else {
			MaxNode newRoot = new MaxNode(state, null);
			for (MinNode min : MProot.children) {
				for (MaxNode max : min.children) {
					if (max.current.equals(state) && newRoot.visits < max.visits) {
						newRoot = max;
					}
				}
			}
			newRoot.parent = null;
			MProot = newRoot;
		}
		double count = 0;
		while (start + 2000 < timeout) {
			MaxNode node  = MPSelect(MProot);
			MaxNode expandNode = MPExpand(node);
			Double score = MPSimulate(expandNode.current);
			count++;
			MPBackpropagate(expandNode, score);
			start = System.currentTimeMillis();
		}
		double high_score = 0;
		System.out.println("number of depth charges: " + count);
		for (MinNode x : MProot.children) {
			System.out.println("Move: " + x.moveTo);
			System.out.println("Utility: " + x.utility);
			System.out.println("Visits: " + x.visits);
			System.out.println("Score: " + (x.utility / x.visits));
			if (x.utility/x.visits >= high_score) {
				selection = x.moveTo;
				high_score = x.utility/x.visits;
			}
		}
		for (Move m : machine.getLegalMoves(state, getRole())) {
			System.out.println(m);
		}
		System.out.println("move is: " + selection );
		System.out.println("...");
		return selection;
	}



	private Move bestSPMove(MachineState state, long timeout)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		long start  = System.currentTimeMillis();

		if (Sproot == null) {
			Sproot = new MonteNode(state);
		}
		Move selection = null;
		double count = 0;
		while (start + 2000 < timeout) {

			MonteNode node = SPSelect(Sproot);
			MonteNode child = SPExpand(node);
			double reward = SPSimulate(child.current);
			count++;
			SPBackpropagate(reward, child);


			start = System.currentTimeMillis();
		}
		double high_score = 0;
		MonteNode newRoot = null;
		System.out.println("...");
		System.out.println("number of depth charges: " + count);
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
//		machine.getMachineStateFromSentenceList(sentenceList)
		return selection;

	}

	private MonteNode SPSelect(MonteNode node)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {

//		List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
		List<Move> moves = machine.getLegalMoves(node.current, getRole());

//		System.out.println(moves);
//		System.out.println(moves2);
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

		if (!machine.isTerminal(node.current)) {

			List<Move> moves = getStateMachine().getLegalMoves(node.current, getRole());
			int existingChildren = node.children.size();
			Move move = moves.get(existingChildren);
			ArrayList<Move> new_moves = new ArrayList<Move>();
			new_moves.add(move);
//			MachineState nextState = machine.getNextState(node.current, new_moves);
			MachineState nextState = machine.getNextState(node.current, new_moves);
//			System.out.println(nextState);
//			System.out.println(nextState2);
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
//		System.out.println(state);
//		System.out.println("reward is " + machine.findReward(getRole(), state) );
		if (machine.isTerminal(state)) {
//			System.out.println(state);
//			System.out.println("found terminal");
//			System.out.println("reward is " + machine.findReward(getRole(), state) );
			return machine.findReward(getRole(), state);

		}
//		if (machine.findTerminalp(state)) {
////		System.out.println(state);
////		System.out.println("found terminal");
////		System.out.println("reward is " + propnet.findReward(getRole(), state) );
//		return machine.findReward(getRole(), state);
//
//		}

//		System.out.println("not found terminal");
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
		MonteMPNode parent = null;
		double utility = 0;
		double visits = 0;
		boolean fullyExpanded = false;
		double fullyExpandedNumber = 0.0;


		private MonteMPNode(MonteMPNode parent) {
			this.parent = parent;
		}
	}

	private class MinNode extends MonteMPNode {
		ArrayList<MaxNode> children = new ArrayList<MaxNode>();
		Boolean hasAllChildren = false;
		Move moveTo;
		private MinNode(MonteMPNode parent, Move moveT) {
			super(parent);
			moveTo = moveT;
		}
	}

	private class MaxNode extends MonteMPNode {
		Boolean hasAllGrandChildren = false;
		ArrayList<MinNode> children = new ArrayList<MinNode>();;
		MachineState current = null;
		private MaxNode(MachineState state, MonteMPNode parent) {
			super(parent);
			current = state;

		}

	}

	MonteNode Sproot = null;
	MaxNode MProot = null;
	StateMachine machine;
//	SamplePropNetStateMachine propnet;


}
