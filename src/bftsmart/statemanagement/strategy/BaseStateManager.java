package bftsmart.statemanagement.strategy;

import java.util.Collection;
import java.util.HashMap;

import bftsmart.reconfiguration.ServerViewManager;
import bftsmart.reconfiguration.views.View;
import bftsmart.statemanagement.ApplicationState;
import bftsmart.statemanagement.SMMessage;
import bftsmart.statemanagement.StateManager;
import bftsmart.tom.core.DeliveryThread;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.util.Logger;

public abstract class BaseStateManager implements StateManager {
	
    protected TOMLayer tomLayer;
    protected ServerViewManager SVManager;
	
    protected HashMap<Integer, ApplicationState> senderStates = null;
    protected HashMap<Integer, View> senderViews = null;
    protected HashMap<Integer, Integer> senderRegencies = null;
    protected HashMap<Integer, Integer> senderLeaders = null;

    protected boolean appStateOnly;
    protected int waitingEid = -1;
    protected int lastEid;
    protected ApplicationState state;

    public BaseStateManager() {
        senderStates = new HashMap<Integer, ApplicationState>();
        senderViews = new HashMap<Integer, View>();
        senderRegencies = new HashMap<Integer, Integer>();
        senderLeaders = new HashMap<Integer, Integer>();
    }
   
    protected int getReplies() {
        return senderStates.size();
    }

    protected boolean moreThanF_Replies() {
    	return senderStates.size() > SVManager.getCurrentViewF();
    }

    protected boolean moreThan2F_Regencies(int regency) {
        return senderRegencies.size() > SVManager.getQuorum2F();
    }
    
    protected boolean moreThan2F_Leaders(int leader) {
        return senderLeaders.size() > SVManager.getQuorum2F();
    }

    protected boolean moreThan2F_Views(View view) {
    	Collection<View> views = senderViews.values();
    	int counter = 0;
    	for(View v : views) {
    		if(view.equals(v))
    			counter++;
    	}
        boolean result = counter > SVManager.getQuorum2F();
        views = null;
        return result;
    }
    
    /**
     * Clear the collections and state hold by this object.
     * Calls clear() in the States, Leaders, Regenviews and Views collections.
     * Sets the state to null; 
     */
    protected void reset() {
        senderStates.clear();
        senderLeaders.clear();
        senderRegencies.clear();
        senderViews.clear();
        state = null;
    }
    
    public Collection<ApplicationState> receivedStates() {
    	return senderStates.values();
    }
    
    public void setLastEID(int eid) {
    	lastEid = eid;
    }
    
    public int getLastEID() {
    	return lastEid;
    }
	
	@Override
    public void requestAppState(int eid) {
    	lastEid = eid + 1;
    	waitingEid = eid;
		System.out.println("waitingeid is now " + eid);
        appStateOnly = true;
        requestState();
    }

	@Override
    public void analyzeState(int eid) {
        Logger.println("(TOMLayer.analyzeState) The state transfer protocol is enabled");
        if (waitingEid == -1) {
            Logger.println("(TOMLayer.analyzeState) I'm not waiting for any state, so I will keep record of this message");
            if (tomLayer.execManager.isDecidable(eid)) {
                System.out.println("(TOMLayer.analyzeState) I have now more than " + SVManager.getCurrentViewF() + " messages for EID " + eid + " which are beyond EID " + lastEid);
                lastEid = eid;
                waitingEid = eid - 1;
        		System.out.println("analyzeState " + waitingEid);
                requestState();
            }
        }
    }

	@Override
	public abstract void init(TOMLayer tomLayer, DeliveryThread dt);
	
	@Override
    public boolean isRetrievingState() {
    	return waitingEid > -1;
    }

	protected abstract void requestState();
	
	@Override
	public abstract void stateTimeout();

	@Override
	public abstract void SMRequestDeliver(SMMessage msg);

	@Override
	public abstract void SMReplyDeliver(SMMessage msg);

}
