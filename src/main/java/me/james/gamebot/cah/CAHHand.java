package me.james.gamebot.cah;

import java.util.*;

public class CAHHand
{
    private final String id;
    private ArrayList<CAHWhiteCard> hand = new ArrayList<>();

    public CAHHand( String id )
    {
        this.id = id;
    }

    public void addCard( CAHWhiteCard card )
    {
        hand.add( card );
    }

    public void removeCard( CAHWhiteCard card )
    {
        hand.remove( card );
    }

    public CAHWhiteCard[] getHand()
    {
        return hand.toArray( new CAHWhiteCard[hand.size()] );
    }

    public String getID()
    {
        return id;
    }
}
