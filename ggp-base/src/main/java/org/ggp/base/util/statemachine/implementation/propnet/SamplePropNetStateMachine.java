package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    private void markBases(List<GdlSentence> marks) {
    	for (GdlSentence key : this.propNet.getBasePropositions().keySet()) {
    		if (marks.contains(key)) {
    			this.propNet.getBasePropositions().get(key).setValue(true);
    		} else {
    			this.propNet.getBasePropositions().get(key).setValue(false);
    		}
    	}
    }

    private void markActions(List<GdlSentence> marks) {
    	for (GdlSentence key : this.propNet.getInputPropositions().keySet()) {
    		if (marks.contains(key)) {
    			this.propNet.getInputPropositions().get(key).setValue(true);
    		} else {
    			this.propNet.getInputPropositions().get(key).setValue(false);
    		}
    	}
    }

    private void clearBases() {
    	for (GdlSentence key : this.propNet.getBasePropositions().keySet()) {
			this.propNet.getBasePropositions().get(key).setValue(false);
    	}
    	for (GdlSentence key : this.propNet.getInputPropositions().keySet()) {
    		this.propNet.getInputPropositions().get(key).setValue(false);
    	}
    }

    private boolean propmarkp(Component pi) {
//    	System.out.println(pi);
    	boolean result = false;
    	if (this.propNet.getBasePropositions().containsValue(pi)) {
    		result = pi.getValue();
    	} else if (pi.equals(this.propNet.getInitProposition())) {
    		result = pi.getValue();
    	} else if (this.propNet.getInputPropositions().containsValue(pi)) {
    		result = pi.getValue();
    	} else if (pi instanceof Not) {
    		result = propmarkNegation(pi);
    	} else if (pi instanceof And) {
    		result = propmarkConjunction(pi);
    	} else if (pi instanceof Or) {
    		result = propmarkDisjunction(pi);
    	} else if (pi instanceof Constant) {
    		result = pi.getValue();
    	} else if (pi instanceof Transition) {
    		result = propmarkp(pi.getSingleInput());
    	} else {
	    	if (pi.getInputs().size() > 0) {
	    		result = propmarkp(pi.getSingleInput());
	    	} else {
	    		System.out.println("ERROR: view prop without 0 input");
	    		result = pi.getValue();
	    	}

    	}
//    	System.out.println(result);
    	return result;
    }

    private boolean propmarkNegation(Component pee) {
    	return !propmarkp(pee.getSingleInput());
    }


    private boolean propmarkConjunction(Component p) {
    	Set<Component> sources = p.getInputs();
    	for (Component source : sources) {
    		if (!propmarkp(source)) {
    			return false;
    		}
    	}
    	return true;
    }

    private boolean propmarkDisjunction(Component p) {
    	Set<Component> sources = p.getInputs();
    	for (Component source : sources) {
    		if (propmarkp(source)) {
    			return true;
    		}
    	}
    	return false;
    }
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) {

    	clearBases();
    	List<GdlSentence> marks = new ArrayList(state.getContents());
    	markBases(marks);
    	//List<Role> roles = this.propNet.getRoles();
    	List<Proposition> actions = new ArrayList();
    	List<Move> moves = new ArrayList();

    	for (Proposition prop : this.propNet.getLegalPropositions().get(role)) {
    		if (propmarkp(prop)) {
//    			System.out.println("found a move");
    			actions.add(prop);
    			moves.add(getMoveFromProposition(prop));
    		}
    	}

    	return moves;
    }


    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	clearBases();
        List<GdlSentence> actions = toDoes(moves);
        markActions(actions);
    	List<GdlSentence> marks = new ArrayList(state.getContents());
    	markBases(marks);
    	Set<GdlSentence> contents = new HashSet<GdlSentence>();
    	for (Map.Entry<GdlSentence, Proposition> entry : this.propNet.getBasePropositions().entrySet()) {
    		Proposition p = entry.getValue();
    		boolean res = propmarkp(p.getSingleInput().getSingleInput());
    		if (res) {
    			contents.add(p.getName());
    		}

    	}
    	MachineState newState = new MachineState(contents);
    	return newState;

    }

    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values()) {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        //System.out.println(contents);
        return new MachineState(contents);
    }

//
//    public MachineState getStateFromBase()
//    {
//        Set<GdlSentence> contents = new HashSet<GdlSentence>();
//        for (Proposition p : propNet.getBasePropositions().values())
//        {
//            p.setValue(p.getSingleInput().getValue());
//            if (p.getValue())
//            {
//                contents.add(p.getName());
//            }
//
//        }
//        return new MachineState(contents);
//    }

    //TODO: make sure this is right. sudo code doesnt match up well
    @Override
	public int findReward(Role role, MachineState state) throws GoalDefinitionException {
    	List<GdlSentence> marks = new ArrayList(state.getContents());
    	clearBases();
    	markBases(marks);
    	Set<Proposition> gprops = this.propNet.getGoalPropositions().get(role);
    	for (Proposition p : gprops) {
    		if (propmarkp(p)) {
    			return getGoalValue(p);
    		}
    	}
    	System.out.println("reached return in findReward");
    	return 0;
    }

    private boolean propterminalp(MachineState state) {
    	return isTerminal(state);
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	List<GdlSentence> marks = new ArrayList(state.getContents());
    	clearBases();
    	markBases(marks);
//    	System.out.println(propNet.getTerminalProposition().getValue());
        return propmarkp(propNet.getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	List<GdlSentence> marks = new ArrayList(state.getContents());
    	clearBases();
    	markBases(marks);
    	Set<Proposition> gprops = this.propNet.getGoalPropositions().get(role);
    	for (Proposition p : gprops) {
    		if (propmarkp(p)) {
    			return getGoalValue(p);
    		}
    	}
    	System.out.println("reached return in findReward");
    	return 0;
    }
    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	for (Proposition val : this.propNet.getBasePropositions().values()) {
    		val.setValue(false);
    	}
    	for (Proposition val  : this.propNet.getInputPropositions().values()) {
    		val.setValue(false);
    	}
    	this.propNet.getInitProposition().setValue(true);
    	MachineState b = getStateFromBase();
    	this.propNet.getInitProposition().setValue(false);
    	return b;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.
        return null;
    }

    /**
     * Computes the legal moves for role in state.
     */



    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.

        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */

}